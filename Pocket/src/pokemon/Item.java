package pokemon;

public class Item {
    private String name;
    private ItemType type;
    private int value;
    private int price;
    private String description;

    public enum ItemType {
        HEAL("恢复"),
        DAMAGE("伤害"),
        EXP_BOOST("经验提升"),
        BALL("精灵球"),
        OTHER("其他");

        private final String chineseName;

        ItemType(String chineseName) {
            this.chineseName = chineseName;
        }

        public String getChineseName() {
            return chineseName;
        }
    }

    public Item(String name, ItemType type, int value, int price, String description) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.price = price;
        this.description = description;
    }

    public String getName() { return name; }
    public ItemType getType() { return type; }
    public int getValue() { return value; }
    public int getPrice() { return price; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return String.format("%s (%s) - 效果:%d 价格:%d元\n  %s",
                name, type.getChineseName(), value, price, description);
    }
}