package cn.gotom.commons.config;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cn.gotom.commons.Item;
import cn.gotom.commons.json.JSON;
import cn.gotom.commons.model.Token;

@Component
public class TokenSecretManager {

	private final static String DATAID = "token-secret";

	@Autowired
	private ConfigManager configManager;

	@PostConstruct
	public void init() {
		configManager.global(DATAID, config -> afterSecretSet(config));
	}

	private void afterSecretSet(String config) {
		List<Item> itemList = JSON.parseList(config, Item.class);
		if (itemList != null) {
			itemList.forEach(item -> Token.addSecret(item.getKey(), item.getValue()));
		}
	}
}
