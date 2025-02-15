package cn.gotom.maxims.redis;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Sentinel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.ReflectionUtils;

import cn.gotom.commons.config.ConfigManager;
import lombok.extern.slf4j.Slf4j;

@Configuration
@ConditionalOnClass({ Redisson.class, RedisOperations.class })
@AutoConfigureBefore(RedisAutoConfiguration.class)
@EnableConfigurationProperties({ RedissonProperties.class, RedisProperties.class })
@Slf4j
public class RedissonAutoConfiguration {

	private static final String REDIS_PROTOCOL_PREFIX = "redis://";
	private static final String REDISS_PROTOCOL_PREFIX = "rediss://";

	@Autowired
	private RedissonProperties redissonProperties;

	@Autowired
	private RedisProperties redisProperties;

	@Autowired
	private ApplicationContext ctx;

	@Bean
	@ConditionalOnMissingBean(name = "redisTemplate")
	public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<Object, Object> template = new RedisTemplate<Object, Object>();
		template.setConnectionFactory(redisConnectionFactory);
		return template;
	}

	@Bean
	@ConditionalOnMissingBean(StringRedisTemplate.class)
	public StringTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		StringTemplate template = new StringTemplate(redisConnectionFactory);
		template.setConnectionFactory(redisConnectionFactory);
		return template;
	}

	@Bean
	@ConditionalOnMissingBean(RedisConnectionFactory.class)
	public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redisson) {
		return new RedissonConnectionFactory(redisson);
	}

	private static final String DATAID = "redisson";

	@Bean
	@Lazy
	@ConditionalOnMissingBean(RedissonReactiveClient.class)
	public RedissonReactiveClient redissonReactive(ConfigManager configManager) {
		RedissonReactive redisson = new RedissonReactive();
		configManager.global(DATAID, //
				config -> redisson.setDelegate(org.redisson.Redisson.createReactive(config(config))));
		return redisson;
	}

	@Bean(destroyMethod = "shutdown")
	@ConditionalOnMissingBean(RedissonClient.class)
	public RedissonClient redissonClient(ConfigManager configManager) {
		try {
			Redisson redisson = new Redisson(config(null));
			configManager.global(DATAID, //
					config -> redisson.setDelegate(org.redisson.Redisson.create(config(config))));
			return redisson;
		} catch (Exception ex) {
			log.warn(ex.getMessage());
			return null;
		}
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private Config config(String configInfo) {
		if (StringUtils.isNotBlank(configInfo)) {
			redissonProperties.setConfig(configInfo);
		}
		Config config = null;
		Method clusterMethod = ReflectionUtils.findMethod(RedisProperties.class, "getCluster");
		Method timeoutMethod = ReflectionUtils.findMethod(RedisProperties.class, "getTimeout");
		Object timeoutValue = ReflectionUtils.invokeMethod(timeoutMethod, redisProperties);
		int timeout;
		if (null == timeoutValue) {
			timeout = 10000;
		} else if (!(timeoutValue instanceof Integer)) {
			Method millisMethod = ReflectionUtils.findMethod(timeoutValue.getClass(), "toMillis");
			timeout = ((Long) ReflectionUtils.invokeMethod(millisMethod, timeoutValue)).intValue();
		} else {
			timeout = (Integer) timeoutValue;
		}

		if (redissonProperties.getConfig() != null) {
			try {
				config = Config.fromYAML(redissonProperties.getConfig());
			} catch (IOException e) {
				try {
					config = Config.fromJSON(redissonProperties.getConfig());
				} catch (IOException e1) {
					throw new IllegalArgumentException("Can't parse config", e1);
				}
			}
		} else if (redissonProperties.getFile() != null) {
			try {
				InputStream is = getConfigStream();
				config = Config.fromYAML(is);
			} catch (IOException e) {
				// trying next format
				try {
					InputStream is = getConfigStream();
					config = Config.fromJSON(is);
				} catch (IOException e1) {
					throw new IllegalArgumentException("Can't parse config", e1);
				}
			}
		} else if (redisProperties.getSentinel() != null) {
			Method nodesMethod = ReflectionUtils.findMethod(Sentinel.class, "getNodes");
			Object nodesValue = ReflectionUtils.invokeMethod(nodesMethod, redisProperties.getSentinel());

			String[] nodes;
			if (nodesValue instanceof String) {
				nodes = convert(Arrays.asList(((String) nodesValue).split(",")));
			} else {
				nodes = convert((List<String>) nodesValue);
			}

			config = new Config();
			config.useSentinelServers().setMasterName(redisProperties.getSentinel().getMaster())
					.addSentinelAddress(nodes).setDatabase(redisProperties.getDatabase()).setConnectTimeout(timeout)
					.setPassword(redisProperties.getPassword());
		} else if (clusterMethod != null && ReflectionUtils.invokeMethod(clusterMethod, redisProperties) != null) {
			Object clusterObject = ReflectionUtils.invokeMethod(clusterMethod, redisProperties);
			Method nodesMethod = ReflectionUtils.findMethod(clusterObject.getClass(), "getNodes");
			List<String> nodesObject = (List<String>) ReflectionUtils.invokeMethod(nodesMethod, clusterObject);
			String[] nodes = convert(nodesObject);

			config = new Config();
			config.useClusterServers().addNodeAddress(nodes).setConnectTimeout(timeout)
					.setPassword(redisProperties.getPassword());
		} else {
			config = new Config();
			String prefix = REDIS_PROTOCOL_PREFIX;
			Method method = ReflectionUtils.findMethod(RedisProperties.class, "isSsl");
			if (method != null && (Boolean) ReflectionUtils.invokeMethod(method, redisProperties)) {
				prefix = REDISS_PROTOCOL_PREFIX;
			}

			config.useSingleServer().setAddress(prefix + redisProperties.getHost() + ":" + redisProperties.getPort())
					.setConnectTimeout(timeout).setDatabase(redisProperties.getDatabase())
					.setPassword(redisProperties.getPassword());
		}
		return config;
	}

	private String[] convert(List<String> nodesObject) {
		List<String> nodes = new ArrayList<String>(nodesObject.size());
		for (String node : nodesObject) {
			if (!node.startsWith(REDIS_PROTOCOL_PREFIX) && !node.startsWith(REDISS_PROTOCOL_PREFIX)) {
				nodes.add(REDIS_PROTOCOL_PREFIX + node);
			} else {
				nodes.add(node);
			}
		}
		return nodes.toArray(new String[nodes.size()]);
	}

	private InputStream getConfigStream() throws IOException {
		Resource resource = ctx.getResource(redissonProperties.getFile());
		InputStream is = resource.getInputStream();
		return is;
	}

}
