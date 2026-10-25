package one.ggsky.alternativeauth.skin;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import one.ggsky.alternativeauth.config.AlternativeAuthConfig;
import one.ggsky.alternativeauth.logger.AlternativeAuthLogger;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerManager;
import one.ggsky.alternativeauth.mineskin.MineskinService;
import one.ggsky.alternativeauth.util.SkinUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class SkinResigner {
    private static final AlternativeAuthLogger LOGGER = AlternativeAuthLoggerManager.getLogger();
    private static final Cache<Integer, Property> SIGNATURE_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(24, TimeUnit.HOURS)
            .maximumSize(1000)
            .build();

    public static PropertyMap resignIfNeeded(AlternativeAuthConfig config, PropertyMap properties, String profileName) {
        if (properties == null || !config.getMineSkin().isEnabled()) {
            return properties;
        }

        Property skinProperty = SkinUtils.getPlayerSkin(properties);
        if (skinProperty == null) {
            return properties;
        }

        SkinUtils.SkinData skinData = SkinUtils.getSkinUrl(skinProperty);
        if (skinData == null || skinData.url() == null) {
            return properties;
        }

        if (SkinUtils.isMojangTexture(skinData.url(), skinProperty)) {
            LOGGER.debug("Skin for " + profileName + " is already hosted on Mojang and signed");
            return properties;
        }

        int propertyHash = skinProperty.value().hashCode();
        Property cached = SIGNATURE_CACHE.getIfPresent(propertyHash);
        if (cached != null) {
            LOGGER.debug("Using cached MineSkin signature for " + profileName);
            return replaceTexturesProperty(properties, cached);
        }

        LOGGER.info("Resigning external skin for " + profileName + " via MineSkin (" + skinData.url() + ")");
        try {
            Optional<Property> resigned = MineskinService.getInstance().signSkin(config.getMineSkin(), skinData.url(), skinData.variant());
            if (resigned.isPresent()) {
                Property newSkin = resigned.get();
                SIGNATURE_CACHE.put(propertyHash, newSkin);
                LOGGER.info("Successfully resigned skin for " + profileName + " via MineSkin");
                return replaceTexturesProperty(properties, newSkin);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to resign skin for " + profileName + " via MineSkin: " + e.getMessage());
        }

        return properties;
    }

    private static PropertyMap replaceTexturesProperty(PropertyMap properties, Property newSkin) {
        Multimap<String, Property> newProperties = LinkedHashMultimap.create(properties);
        newProperties.removeAll("textures");
        if (newSkin != null) {
            newProperties.put("textures", newSkin);
        }
        return new PropertyMap(newProperties);
    }
}
