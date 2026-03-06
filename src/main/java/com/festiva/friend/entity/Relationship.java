package com.festiva.friend.entity;

import com.festiva.i18n.Lang;

public enum Relationship {
    PARTNER("💑 Partner",      "💑 Партнёр"),
    WIFE("👰 Wife",            "👰 Жена"),
    HUSBAND("🤵 Husband",      "🤵 Муж"),
    MUM("👩 Mum",              "👩 Мама"),
    DAD("👨 Dad",              "👨 Папа"),
    SISTER("👧 Sister",        "👧 Сестра"),
    BROTHER("👦 Brother",      "👦 Брат"),
    DAUGHTER("🧒 Daughter",    "🧒 Дочь"),
    SON("👦 Son",              "👦 Сын"),
    GRANDMOTHER("👵 Grandma",  "👵 Бабушка"),
    GRANDFATHER("👴 Grandpa",  "👴 Дедушка"),
    AUNT("👩 Aunt",            "👩 Тётя"),
    UNCLE("👨 Uncle",          "👨 Дядя"),
    COUSIN("🧑 Cousin",        "🧑 Кузен/а"),
    FRIEND("👫 Friend",        "👫 Друг"),
    BEST_FRIEND("🌟 Best Friend", "🌟 Лучший друг"),
    COLLEAGUE("🤝 Colleague",  "🤝 Коллега"),
    CLASSMATE("🎓 Classmate",  "🎓 Одноклассник"),
    NEIGHBOUR("🏠 Neighbour",  "🏠 Сосед"),
    OTHER("👤 Other",          "👤 Другое");

    public final String labelEn;
    public final String labelRu;

    Relationship(String labelEn, String labelRu) {
        this.labelEn = labelEn;
        this.labelRu = labelRu;
    }

    public String label(Lang lang) {
        return lang == Lang.EN ? labelEn : labelRu;
    }

    // backward compat — used in /list display
    public String getLabel(Lang lang) { return label(lang); }
}
