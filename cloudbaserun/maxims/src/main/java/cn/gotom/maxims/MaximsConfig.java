package cn.gotom.maxims;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@EntityScan({ "com.zhnaste.commons", "com.zhnaste.ems" })
@Configuration
public class MaximsConfig {

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration cfg = new CorsConfiguration();
		cfg.setAllowCredentials(true);
		// cfg.addAllowedOrigin("*");
		cfg.addAllowedOriginPattern("*");
		cfg.addAllowedMethod("*");
		cfg.addAllowedHeader("*");
		cfg.addExposedHeader("*");
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", cfg);
		return source;
	}

	@Bean
	public CorsWebFilter CorsWebFilter(@Autowired CorsConfigurationSource corsConfigurationSource) {
		return new CorsWebFilter(corsConfigurationSource);
	}
}
