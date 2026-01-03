package pokemon;

import java.util.*;

public class PocketMon {
    // 经验值升级表
    private static final int[] EXP_REQUIREMENTS = {
            0, 50, 100, 200, 350, 500, 700, 900, 1150, 1400,
            1700, 2000, 2350, 2700, 3100, 3500, 4000, 4500, 5000, 5500, 6000
    };

    private String name;
    private Type type;
    private int level;
    private int maxHp;
    private int currentHp;
    private int attack;
    private int defense;
    private int exp;
    private List<Skill> skills;

    public enum Type {
        GRASS, FIRE, WATER, ELECTRIC, BUG, FLYING, NORMAL
    }

    public PocketMon(String name, Type type, int level) {
        this.name = name;
        this.type = type;
        this.level = level;
        this.skills = new ArrayList<>();

        calculateStats();
        this.currentHp = this.maxHp;

        // 初始经验
        this.exp = level > 1 && level <= EXP_REQUIREMENTS.length ? EXP_REQUIREMENTS[level - 1] : 0;

        initializeSkills();
    }

    private void calculateStats() {
        this.maxHp = 30 + (level * 5);
        this.attack = 10 + (level * 2);
        this.defense = 10 + (level * 2);
    }

    private void initializeSkills() {
        skills.add(new Skill("撞击", Type.NORMAL, 35, 100, 35, Skill.SkillCategory.PHYSICAL, 1));

        switch (this.type) {
            case GRASS:
                skills.add(new Skill("藤鞭", Type.GRASS, 45, 100, 25, Skill.SkillCategory.PHYSICAL, 3));
                break;
            case FIRE:
                skills.add(new Skill("火花", Type.FIRE, 40, 100, 25, Skill.SkillCategory.SPECIAL, 3));
                break;
            case WATER:
                skills.add(new Skill("水枪", Type.WATER, 40, 100, 25, Skill.SkillCategory.SPECIAL, 3));
                break;
            case ELECTRIC:
                skills.add(new Skill("电击", Type.ELECTRIC, 40, 100, 25, Skill.SkillCategory.SPECIAL, 3));
                break;
            case BUG:
                skills.add(new Skill("吐丝", Type.BUG, 0, 95, 40, Skill.SkillCategory.STATUS, 1));
                break;
            case FLYING:
                skills.add(new Skill("起风", Type.FLYING, 40, 100, 35, Skill.SkillCategory.SPECIAL, 1));
                break;
            default:
                break;
        }
    }

    // 战斗相关
    public void takeDamage(int damage) {
        this.currentHp -= damage;
        if (this.currentHp < 0) this.currentHp = 0;
    }

    public void heal(int amount) {
        this.currentHp += amount;
        if (this.currentHp > this.maxHp) this.currentHp = this.maxHp;
    }

    public void fullHeal() {
        this.currentHp = this.maxHp;
    }

    public boolean isFainted() {
        return this.currentHp <= 0;
    }

    // === 经验与升级 ===
    public void gainExp(int gainedExp) {
        if (level >= EXP_REQUIREMENTS.length) return;
        this.exp += gainedExp;
        while (level < EXP_REQUIREMENTS.length && exp >= EXP_REQUIREMENTS[level]) {
            levelUp();
        }
    }

    private void levelUp() {
        this.level++;
        int oldMaxHp = maxHp;
        calculateStats();
        double hpRatio = (double) currentHp / oldMaxHp;
        currentHp = (int) (maxHp * hpRatio);
        if (currentHp < 1) currentHp = 1;
    }


    public int getExpToNextLevel() {
        if (level >= EXP_REQUIREMENTS.length) return 0;
        return EXP_REQUIREMENTS[level] - exp;
    }

    public String getExpInfo() {
        int need = getExpToNextLevel();
        if (need == 0) return "已达到最高级";
        return "距离下一级: " + need + "经验";
    }

    public Skill getSkill(int index) {
        if (index >= 0 && index < skills.size()) {
            return skills.get(index);
        }
        return null;
    }

    // Getters
    public int getHp() { return currentHp; }
    public int getCurrentHp() { return currentHp; }
    public int getMaxHp() { return maxHp; }
    public String getName() { return name; }
    public Type getType() { return type; }
    public int getLevel() { return level; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public List<Skill> getSkills() { return skills; }

    public String getBattleStatus() {
        return String.format("%s Lv.%d (HP: %d/%d)", name, level, currentHp, maxHp);
    }
}