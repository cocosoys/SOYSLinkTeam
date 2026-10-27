package soys.soyslinkteam.spring.entity.vo;

import java.util.List;

/**
 * 增幅可选项视图：全部药水效果 + buff.yml 配置的属性键 + 支持的属性插件名。
 */
public class BuffOptionsVO {

    private List<Option> potions;
    private List<Option> attributes;
    private List<String> attributePlugins;

    /** 一个可选项（key + 显示名）。 */
    public static class Option {
        private String key;
        private String name;

        public Option() {
        }

        public Option(String key, String name) {
            this.key = key;
            this.name = name;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public List<Option> getPotions() {
        return potions;
    }

    public void setPotions(List<Option> potions) {
        this.potions = potions;
    }

    public List<Option> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Option> attributes) {
        this.attributes = attributes;
    }

    public List<String> getAttributePlugins() {
        return attributePlugins;
    }

    public void setAttributePlugins(List<String> attributePlugins) {
        this.attributePlugins = attributePlugins;
    }
}
