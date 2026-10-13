.class public final Leu/xiaomi/util/JSONTranslator;
.super Ljava/lang/Object;
.source "JSONTranslator.java"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Leu/xiaomi/util/JSONTranslator$SingletonHolder;,
        Leu/xiaomi/util/JSONTranslator$JSONState;
    }
.end annotation


# static fields
.field private static final blacklist MODE_FETCH:I = 0x0

.field private static final blacklist MODE_REPLACE:I = 0x1

.field private static final blacklist TAG:Ljava/lang/String; = "JSONTranslator"


# direct methods
.method private constructor blacklist <init>()V
    .registers 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method synthetic constructor blacklist <init>(Leu/xiaomi/util/JSONTranslator-IA;)V
    .registers 2

    invoke-direct {p0}, Leu/xiaomi/util/JSONTranslator;-><init>()V

    return-void
.end method

.method public static whitelist getInstance()Leu/xiaomi/util/JSONTranslator;
    .registers 1

    invoke-static {}, Leu/xiaomi/util/JSONTranslator$SingletonHolder;->-$$Nest$sfgetINSTANCE()Leu/xiaomi/util/JSONTranslator;

    move-result-object v0

    return-object v0
.end method

.method private blacklist parse(ILeu/xiaomi/util/JSONTranslator$JSONState;Ljava/lang/Object;Ljava/lang/String;ILjava/lang/Object;I)I
    .registers 26
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Lorg/json/JSONException;
        }
    .end annotation

    move/from16 v1, p1

    move-object/from16 v2, p2

    move-object/from16 v0, p3

    move-object/from16 v4, p4

    move/from16 v3, p5

    move-object/from16 v5, p6

    instance-of v6, v5, Lorg/json/JSONArray;

    if-eqz v6, :cond_25

    move-object v3, v5

    check-cast v3, Lorg/json/JSONArray;

    move-object/from16 v0, p0

    move/from16 v1, p1

    move-object/from16 v2, p2

    move-object/from16 v4, p4

    move/from16 v5, p7

    invoke-direct/range {v0 .. v5}, Leu/xiaomi/util/JSONTranslator;->parse(ILeu/xiaomi/util/JSONTranslator$JSONState;Lorg/json/JSONArray;Ljava/lang/String;I)I

    move-result v0

    move-object/from16 v6, p0

    goto/16 :goto_13d

    :cond_25
    instance-of v6, v5, Lorg/json/JSONObject;

    if-eqz v6, :cond_36

    move-object v0, v5

    check-cast v0, Lorg/json/JSONObject;

    move-object/from16 v6, p0

    move/from16 v7, p7

    invoke-direct {v6, v1, v2, v0, v7}, Leu/xiaomi/util/JSONTranslator;->parse(ILeu/xiaomi/util/JSONTranslator$JSONState;Lorg/json/JSONObject;I)I

    move-result v0

    goto/16 :goto_13d

    :cond_36
    move-object/from16 v6, p0

    move/from16 v7, p7

    instance-of v8, v5, Ljava/lang/String;

    if-eqz v8, :cond_13c

    iget-object v8, v2, Leu/xiaomi/util/JSONTranslator$JSONState;->keyList:Ljava/util/List;

    const/4 v9, -0x1

    if-eqz v8, :cond_4a

    iget-object v8, v2, Leu/xiaomi/util/JSONTranslator$JSONState;->keyList:Ljava/util/List;

    invoke-interface {v8, v4}, Ljava/util/List;->indexOf(Ljava/lang/Object;)I

    move-result v8

    goto :goto_4b

    :cond_4a
    move v8, v9

    :goto_4b
    iget-object v10, v2, Leu/xiaomi/util/JSONTranslator$JSONState;->keyList:Ljava/util/List;

    if-eqz v10, :cond_51

    if-le v8, v9, :cond_13c

    :cond_51
    check-cast v5, Ljava/lang/String;

    invoke-static {v5}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v10

    if-nez v10, :cond_13c

    iget-object v10, v2, Leu/xiaomi/util/JSONTranslator$JSONState;->delimPattern:[Ljava/util/regex/Pattern;

    const/4 v11, 0x0

    const/4 v12, 0x1

    if-nez v10, :cond_61

    const/4 v8, 0x0

    goto :goto_6f

    :cond_61
    iget-object v10, v2, Leu/xiaomi/util/JSONTranslator$JSONState;->delimPattern:[Ljava/util/regex/Pattern;

    array-length v10, v10

    if-ne v10, v12, :cond_6b

    iget-object v8, v2, Leu/xiaomi/util/JSONTranslator$JSONState;->delimPattern:[Ljava/util/regex/Pattern;

    aget-object v8, v8, v11

    goto :goto_6f

    :cond_6b
    iget-object v10, v2, Leu/xiaomi/util/JSONTranslator$JSONState;->delimPattern:[Ljava/util/regex/Pattern;

    aget-object v8, v10, v8

    :goto_6f
    if-nez v8, :cond_76

    new-array v10, v12, [Ljava/lang/String;

    aput-object v5, v10, v11

    goto :goto_7a

    :cond_76
    invoke-virtual {v8, v5}, Ljava/util/regex/Pattern;->split(Ljava/lang/CharSequence;)[Ljava/lang/String;

    move-result-object v10

    :goto_7a
    move v5, v11

    :goto_7b
    array-length v13, v10

    if-ge v5, v13, :cond_13c

    if-nez v1, :cond_b3

    aget-object v13, v10, v5

    invoke-virtual {v13}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v13

    iget-object v14, v2, Leu/xiaomi/util/JSONTranslator$JSONState;->textSet:Ljava/util/Set;

    invoke-interface {v14, v13}, Ljava/util/Set;->contains(Ljava/lang/Object;)Z

    move-result v14

    if-eqz v14, :cond_9a

    iget-object v14, v2, Leu/xiaomi/util/JSONTranslator$JSONState;->textList:Ljava/util/List;

    invoke-interface {v14, v13}, Ljava/util/List;->indexOf(Ljava/lang/Object;)I

    move-result v13

    move/from16 v17, v13

    move v13, v7

    move/from16 v7, v17

    goto :goto_a6

    :cond_9a
    iget-object v14, v2, Leu/xiaomi/util/JSONTranslator$JSONState;->textSet:Ljava/util/Set;

    invoke-interface {v14, v13}, Ljava/util/Set;->add(Ljava/lang/Object;)Z

    iget-object v14, v2, Leu/xiaomi/util/JSONTranslator$JSONState;->textList:Ljava/util/List;

    invoke-interface {v14, v13}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    add-int/lit8 v13, v7, 0x1

    :goto_a6
    iget-object v14, v2, Leu/xiaomi/util/JSONTranslator$JSONState;->indexList:Ljava/util/List;

    invoke-static {v7}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v7

    invoke-interface {v14, v7}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    move v12, v9

    move v7, v13

    goto/16 :goto_136

    :cond_b3
    if-ne v1, v12, :cond_135

    iget-object v13, v2, Leu/xiaomi/util/JSONTranslator$JSONState;->indexList:Ljava/util/List;

    add-int/lit8 v14, v7, 0x1

    invoke-interface {v13, v7}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v7

    check-cast v7, Ljava/lang/Integer;

    invoke-virtual {v7}, Ljava/lang/Integer;->intValue()I

    move-result v7

    iget-object v13, v2, Leu/xiaomi/util/JSONTranslator$JSONState;->textList:Ljava/util/List;

    invoke-interface {v13, v7}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v7

    check-cast v7, Ljava/lang/String;

    array-length v13, v10

    if-le v13, v12, :cond_121

    instance-of v13, v0, Lorg/json/JSONArray;

    if-eqz v13, :cond_dc

    move-object v13, v0

    check-cast v13, Lorg/json/JSONArray;

    invoke-virtual {v13, v3}, Lorg/json/JSONArray;->get(I)Ljava/lang/Object;

    move-result-object v13

    check-cast v13, Ljava/lang/String;

    goto :goto_e5

    :cond_dc
    move-object v13, v0

    check-cast v13, Lorg/json/JSONObject;

    invoke-virtual {v13, v4}, Lorg/json/JSONObject;->get(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v13

    check-cast v13, Ljava/lang/String;

    :goto_e5
    invoke-virtual {v8, v13}, Ljava/util/regex/Pattern;->matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;

    move-result-object v15

    move v12, v11

    :goto_ea
    if-ge v12, v5, :cond_f2

    invoke-virtual {v15}, Ljava/util/regex/Matcher;->find()Z

    add-int/lit8 v12, v12, 0x1

    goto :goto_ea

    :cond_f2
    if-lez v12, :cond_f9

    invoke-virtual {v15}, Ljava/util/regex/Matcher;->end()I

    move-result v12

    goto :goto_fa

    :cond_f9
    move v12, v11

    :goto_fa
    invoke-virtual {v15}, Ljava/util/regex/Matcher;->find()Z

    move-result v16

    if-eqz v16, :cond_105

    invoke-virtual {v15}, Ljava/util/regex/Matcher;->start()I

    move-result v15

    goto :goto_106

    :cond_105
    move v15, v9

    :goto_106
    new-instance v9, Ljava/lang/StringBuilder;

    invoke-virtual {v13, v11, v12}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v12

    invoke-direct {v9, v12}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v9, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const/4 v12, -0x1

    if-le v15, v12, :cond_11c

    invoke-virtual {v13, v15}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object v7

    invoke-virtual {v9, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    :cond_11c
    invoke-virtual {v9}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v7

    goto :goto_122

    :cond_121
    move v12, v9

    :goto_122
    instance-of v9, v0, Lorg/json/JSONArray;

    if-eqz v9, :cond_12d

    move-object v9, v0

    check-cast v9, Lorg/json/JSONArray;

    invoke-virtual {v9, v3, v7}, Lorg/json/JSONArray;->put(ILjava/lang/Object;)Lorg/json/JSONArray;

    goto :goto_133

    :cond_12d
    move-object v9, v0

    check-cast v9, Lorg/json/JSONObject;

    invoke-virtual {v9, v4, v7}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    :goto_133
    move v7, v14

    goto :goto_136

    :cond_135
    move v12, v9

    :goto_136
    add-int/lit8 v5, v5, 0x1

    move v9, v12

    const/4 v12, 0x1

    goto/16 :goto_7b

    :cond_13c
    move v0, v7

    :goto_13d
    return v0
.end method

.method private blacklist parse(ILeu/xiaomi/util/JSONTranslator$JSONState;Lorg/json/JSONArray;Ljava/lang/String;I)I
    .registers 16
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Lorg/json/JSONException;
        }
    .end annotation

    invoke-virtual {p3}, Lorg/json/JSONArray;->length()I

    move-result v0

    const/4 v1, 0x0

    move v9, p5

    :goto_6
    if-ge v1, v0, :cond_19

    invoke-virtual {p3, v1}, Lorg/json/JSONArray;->get(I)Ljava/lang/Object;

    move-result-object v8

    move-object v2, p0

    move v3, p1

    move-object v4, p2

    move-object v5, p3

    move-object v6, p4

    move v7, v1

    invoke-direct/range {v2 .. v9}, Leu/xiaomi/util/JSONTranslator;->parse(ILeu/xiaomi/util/JSONTranslator$JSONState;Ljava/lang/Object;Ljava/lang/String;ILjava/lang/Object;I)I

    move-result v9

    add-int/lit8 v1, v1, 0x1

    goto :goto_6

    :cond_19
    return v9
.end method

.method private blacklist parse(ILeu/xiaomi/util/JSONTranslator$JSONState;Lorg/json/JSONObject;I)I
    .registers 14
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Lorg/json/JSONException;
        }
    .end annotation

    invoke-virtual {p3}, Lorg/json/JSONObject;->keys()Ljava/util/Iterator;

    move-result-object v0

    move v8, p4

    :goto_5
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result p4

    if-eqz p4, :cond_20

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object p4

    move-object v5, p4

    check-cast v5, Ljava/lang/String;

    invoke-virtual {p3, v5}, Lorg/json/JSONObject;->get(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v7

    const/4 v6, -0x1

    move-object v1, p0

    move v2, p1

    move-object v3, p2

    move-object v4, p3

    invoke-direct/range {v1 .. v8}, Leu/xiaomi/util/JSONTranslator;->parse(ILeu/xiaomi/util/JSONTranslator$JSONState;Ljava/lang/Object;Ljava/lang/String;ILjava/lang/Object;I)I

    move-result v8

    goto :goto_5

    :cond_20
    return v8
.end method

.method private blacklist translateInternal(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;
    .registers 18
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Lorg/json/JSONException;,
            Ljava/io/IOException;
        }
    .end annotation

    move-object v6, p0

    move-object v0, p3

    move-object/from16 v1, p4

    move-object/from16 v2, p5

    const/4 v7, 0x0

    invoke-virtual {p3, v7}, Ljava/lang/String;->charAt(I)C

    move-result v3

    const/16 v4, 0x5b

    const/4 v8, 0x1

    const/4 v5, 0x0

    if-ne v3, v4, :cond_13

    move v9, v8

    goto :goto_1c

    :cond_13
    invoke-virtual {p3, v7}, Ljava/lang/String;->charAt(I)C

    move-result v3

    const/16 v4, 0x7b

    if-ne v3, v4, :cond_88

    move v9, v7

    :goto_1c
    if-eqz v1, :cond_22

    array-length v3, v1

    if-nez v3, :cond_22

    move-object v1, v5

    :cond_22
    if-eqz v2, :cond_28

    array-length v3, v2

    if-nez v3, :cond_28

    move-object v2, v5

    :cond_28
    if-eqz v2, :cond_42

    array-length v3, v2

    new-array v5, v3, [Ljava/util/regex/Pattern;

    move v3, v7

    :goto_2e
    array-length v4, v2

    if-ge v3, v4, :cond_42

    aget-object v4, v2, v3

    invoke-static {v4}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v10

    if-nez v10, :cond_3f

    :try_start_39
    invoke-static {v4}, Ljava/util/regex/Pattern;->compile(Ljava/lang/String;)Ljava/util/regex/Pattern;

    move-result-object v4

    aput-object v4, v5, v3
    :try_end_3f
    .catch Ljava/util/regex/PatternSyntaxException; {:try_start_39 .. :try_end_3f} :catch_3f

    :catch_3f
    :cond_3f
    add-int/lit8 v3, v3, 0x1

    goto :goto_2e

    :cond_42
    new-instance v10, Leu/xiaomi/util/JSONTranslator$JSONState;

    invoke-direct {v10, v1, v5}, Leu/xiaomi/util/JSONTranslator$JSONState;-><init>([Ljava/lang/String;[Ljava/util/regex/Pattern;)V

    if-eqz v9, :cond_5b

    new-instance v11, Lorg/json/JSONArray;

    invoke-direct {v11, p3}, Lorg/json/JSONArray;-><init>(Ljava/lang/String;)V

    move-object v0, v11

    check-cast v0, Lorg/json/JSONArray;

    const/4 v4, 0x0

    const/4 v5, 0x0

    const/4 v1, 0x0

    move-object v0, p0

    move-object v2, v10

    move-object v3, v11

    invoke-direct/range {v0 .. v5}, Leu/xiaomi/util/JSONTranslator;->parse(ILeu/xiaomi/util/JSONTranslator$JSONState;Lorg/json/JSONArray;Ljava/lang/String;I)I

    goto :goto_66

    :cond_5b
    new-instance v11, Lorg/json/JSONObject;

    invoke-direct {v11, p3}, Lorg/json/JSONObject;-><init>(Ljava/lang/String;)V

    move-object v0, v11

    check-cast v0, Lorg/json/JSONObject;

    invoke-direct {p0, v7, v10, v11, v7}, Leu/xiaomi/util/JSONTranslator;->parse(ILeu/xiaomi/util/JSONTranslator$JSONState;Lorg/json/JSONObject;I)I

    :goto_66
    invoke-static {}, Leu/xiaomi/util/Translator;->getInstance()Leu/xiaomi/util/Translator;

    move-result-object v0

    move-object v1, p1

    move-object v2, p2

    invoke-virtual {v0, p1, p2, v10}, Leu/xiaomi/util/Translator;->translate(Ljava/lang/String;Ljava/lang/String;Leu/xiaomi/util/Translator$State;)V

    if-eqz v9, :cond_7d

    move-object v3, v11

    check-cast v3, Lorg/json/JSONArray;

    const/4 v4, 0x0

    const/4 v5, 0x0

    const/4 v1, 0x1

    move-object v0, p0

    move-object v2, v10

    invoke-direct/range {v0 .. v5}, Leu/xiaomi/util/JSONTranslator;->parse(ILeu/xiaomi/util/JSONTranslator$JSONState;Lorg/json/JSONArray;Ljava/lang/String;I)I

    goto :goto_83

    :cond_7d
    move-object v0, v11

    check-cast v0, Lorg/json/JSONObject;

    invoke-direct {p0, v8, v10, v0, v7}, Leu/xiaomi/util/JSONTranslator;->parse(ILeu/xiaomi/util/JSONTranslator$JSONState;Lorg/json/JSONObject;I)I

    :goto_83
    invoke-virtual {v11}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object v0

    return-object v0

    :cond_88
    return-object v5
.end method


# virtual methods
.method public whitelist translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .registers 10

    const/4 v5, 0x0

    move-object v0, v5

    check-cast v0, [Ljava/lang/String;

    const/4 v4, 0x0

    move-object v0, p0

    move-object v1, p1

    move-object v2, p2

    move-object v3, p3

    invoke-virtual/range {v0 .. v5}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .registers 12

    const/4 v0, 0x1

    new-array v6, v0, [Ljava/lang/String;

    const/4 v0, 0x0

    aput-object p4, v6, v0

    const/4 v5, 0x0

    move-object v1, p0

    move-object v2, p1

    move-object v3, p2

    move-object v4, p3

    invoke-virtual/range {v1 .. v6}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;
    .registers 11

    const/4 v5, 0x0

    move-object v0, v5

    check-cast v0, [Ljava/lang/String;

    move-object v0, p0

    move-object v1, p1

    move-object v2, p2

    move-object v3, p3

    move-object v4, p4

    invoke-virtual/range {v0 .. v5}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .registers 13

    const/4 v0, 0x1

    new-array v6, v0, [Ljava/lang/String;

    const/4 v0, 0x0

    aput-object p5, v6, v0

    move-object v1, p0

    move-object v2, p1

    move-object v3, p2

    move-object v4, p3

    move-object v5, p4

    invoke-virtual/range {v1 .. v6}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;
    .registers 14

    if-eqz p3, :cond_31

    if-eqz p5, :cond_f

    if-eqz p4, :cond_31

    array-length v0, p5

    const/4 v1, 0x1

    if-eq v0, v1, :cond_f

    array-length v0, p4

    array-length v1, p5

    if-eq v0, v1, :cond_f

    goto :goto_31

    :cond_f
    :try_start_f
    invoke-virtual {p3}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object p3

    invoke-virtual {p3}, Ljava/lang/String;->length()I

    move-result v0

    if-lez v0, :cond_31

    move-object v2, p0

    move-object v3, p1

    move-object v4, p2

    move-object v5, p3

    move-object v6, p4

    move-object v7, p5

    invoke-direct/range {v2 .. v7}, Leu/xiaomi/util/JSONTranslator;->translateInternal(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1
    :try_end_23
    .catch Ljava/lang/Exception; {:try_start_f .. :try_end_23} :catch_27

    if-eqz p1, :cond_31

    move-object p3, p1

    goto :goto_31

    :catch_27
    move-exception p1

    const-string p2, "JSONTranslator"

    invoke-virtual {p1}, Ljava/lang/Exception;->getMessage()Ljava/lang/String;

    move-result-object p4

    invoke-static {p2, p4, p1}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    :cond_31
    :goto_31
    return-object p3
.end method

.method public whitelist translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;)V
    .registers 10

    const/4 v5, 0x0

    move-object v0, v5

    check-cast v0, [Ljava/lang/String;

    const/4 v4, 0x0

    move-object v0, p0

    move-object v1, p1

    move-object v2, p2

    move-object v3, p3

    invoke-virtual/range {v0 .. v5}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;[Ljava/lang/String;)V

    return-void
.end method

.method public whitelist translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;Ljava/lang/String;)V
    .registers 12

    const/4 v0, 0x1

    new-array v6, v0, [Ljava/lang/String;

    const/4 v0, 0x0

    aput-object p4, v6, v0

    const/4 v5, 0x0

    move-object v1, p0

    move-object v2, p1

    move-object v3, p2

    move-object v4, p3

    invoke-virtual/range {v1 .. v6}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;[Ljava/lang/String;)V

    return-void
.end method

.method public whitelist translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;)V
    .registers 11

    const/4 v5, 0x0

    move-object v0, v5

    check-cast v0, [Ljava/lang/String;

    move-object v0, p0

    move-object v1, p1

    move-object v2, p2

    move-object v3, p3

    move-object v4, p4

    invoke-virtual/range {v0 .. v5}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;[Ljava/lang/String;)V

    return-void
.end method

.method public whitelist translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;Ljava/lang/String;)V
    .registers 13

    const/4 v0, 0x1

    new-array v6, v0, [Ljava/lang/String;

    const/4 v0, 0x0

    aput-object p5, v6, v0

    move-object v1, p0

    move-object v2, p1

    move-object v3, p2

    move-object v4, p3

    move-object v5, p4

    invoke-virtual/range {v1 .. v6}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;[Ljava/lang/String;)V

    return-void
.end method

.method public whitelist translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;[Ljava/lang/String;)V
    .registers 13

    if-eqz p3, :cond_36

    if-eqz p5, :cond_f

    if-eqz p4, :cond_36

    array-length v0, p5

    const/4 v1, 0x1

    if-eq v0, v1, :cond_f

    array-length v0, p4

    array-length v1, p5

    if-eq v0, v1, :cond_f

    goto :goto_36

    :cond_f
    :try_start_f
    invoke-static {p3}, Leu/xiaomi/util/FileUtil;->readFile(Ljava/io/File;)Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/String;->length()I

    move-result v0

    if-lez v0, :cond_36

    move-object v1, p0

    move-object v2, p1

    move-object v3, p2

    move-object v5, p4

    move-object v6, p5

    invoke-direct/range {v1 .. v6}, Leu/xiaomi/util/JSONTranslator;->translateInternal(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    if-eqz p1, :cond_36

    invoke-static {p3, p1}, Leu/xiaomi/util/FileUtil;->writeFile(Ljava/io/File;Ljava/lang/String;)V
    :try_end_2b
    .catch Ljava/lang/Exception; {:try_start_f .. :try_end_2b} :catch_2c

    goto :goto_36

    :catch_2c
    move-exception p1

    const-string p2, "JSONTranslator"

    invoke-virtual {p1}, Ljava/lang/Exception;->getMessage()Ljava/lang/String;

    move-result-object p3

    invoke-static {p2, p3, p1}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    :cond_36
    :goto_36
    return-void
.end method

.method public whitelist translateAuto(Ljava/lang/String;)Ljava/lang/String;
    .registers 3

    const/4 v0, 0x0

    invoke-virtual {p0, v0, v0, p1}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translateAuto(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .registers 4

    const/4 v0, 0x0

    invoke-virtual {p0, v0, v0, p1, p2}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translateAuto(Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;
    .registers 4

    const/4 v0, 0x0

    invoke-virtual {p0, v0, v0, p1, p2}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translateAuto(Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .registers 10

    const/4 v1, 0x0

    const/4 v2, 0x0

    move-object v0, p0

    move-object v3, p1

    move-object v4, p2

    move-object v5, p3

    invoke-virtual/range {v0 .. v5}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translateAuto(Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;
    .registers 10

    const/4 v1, 0x0

    const/4 v2, 0x0

    move-object v0, p0

    move-object v3, p1

    move-object v4, p2

    move-object v5, p3

    invoke-virtual/range {v0 .. v5}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translateAuto(Ljava/io/File;)V
    .registers 3

    const/4 v0, 0x0

    invoke-virtual {p0, v0, v0, p1}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;)V

    return-void
.end method

.method public whitelist translateAuto(Ljava/io/File;Ljava/lang/String;)V
    .registers 4

    const/4 v0, 0x0

    invoke-virtual {p0, v0, v0, p1, p2}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;Ljava/lang/String;)V

    return-void
.end method

.method public whitelist translateAuto(Ljava/io/File;[Ljava/lang/String;)V
    .registers 4

    const/4 v0, 0x0

    invoke-virtual {p0, v0, v0, p1, p2}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;)V

    return-void
.end method

.method public whitelist translateAuto(Ljava/io/File;[Ljava/lang/String;Ljava/lang/String;)V
    .registers 10

    const/4 v1, 0x0

    const/4 v2, 0x0

    move-object v0, p0

    move-object v3, p1

    move-object v4, p2

    move-object v5, p3

    invoke-virtual/range {v0 .. v5}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;Ljava/lang/String;)V

    return-void
.end method

.method public whitelist translateAuto(Ljava/io/File;[Ljava/lang/String;[Ljava/lang/String;)V
    .registers 10

    const/4 v1, 0x0

    const/4 v2, 0x0

    move-object v0, p0

    move-object v3, p1

    move-object v4, p2

    move-object v5, p3

    invoke-virtual/range {v0 .. v5}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;[Ljava/lang/String;)V

    return-void
.end method

.method public whitelist translateFrom(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .registers 4

    const/4 v0, 0x0

    invoke-virtual {p0, p1, v0, p2}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translateFrom(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .registers 5

    const/4 v0, 0x0

    invoke-virtual {p0, p1, v0, p2, p3}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translateFrom(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;
    .registers 5

    const/4 v0, 0x0

    invoke-virtual {p0, p1, v0, p2, p3}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translateFrom(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .registers 11

    const/4 v2, 0x0

    move-object v0, p0

    move-object v1, p1

    move-object v3, p2

    move-object v4, p3

    move-object v5, p4

    invoke-virtual/range {v0 .. v5}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translateFrom(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;
    .registers 11

    const/4 v2, 0x0

    move-object v0, p0

    move-object v1, p1

    move-object v3, p2

    move-object v4, p3

    move-object v5, p4

    invoke-virtual/range {v0 .. v5}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translateFrom(Ljava/lang/String;Ljava/io/File;)V
    .registers 4

    const/4 v0, 0x0

    invoke-virtual {p0, p1, v0, p2}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;)V

    return-void
.end method

.method public whitelist translateFrom(Ljava/lang/String;Ljava/io/File;Ljava/lang/String;)V
    .registers 5

    const/4 v0, 0x0

    invoke-virtual {p0, p1, v0, p2, p3}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;Ljava/lang/String;)V

    return-void
.end method

.method public whitelist translateFrom(Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;)V
    .registers 5

    const/4 v0, 0x0

    invoke-virtual {p0, p1, v0, p2, p3}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;)V

    return-void
.end method

.method public whitelist translateFrom(Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;Ljava/lang/String;)V
    .registers 11

    const/4 v2, 0x0

    move-object v0, p0

    move-object v1, p1

    move-object v3, p2

    move-object v4, p3

    move-object v5, p4

    invoke-virtual/range {v0 .. v5}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;Ljava/lang/String;)V

    return-void
.end method

.method public whitelist translateFrom(Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;[Ljava/lang/String;)V
    .registers 11

    const/4 v2, 0x0

    move-object v0, p0

    move-object v1, p1

    move-object v3, p2

    move-object v4, p3

    move-object v5, p4

    invoke-virtual/range {v0 .. v5}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;[Ljava/lang/String;)V

    return-void
.end method

.method public whitelist translateTo(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .registers 4

    const/4 v0, 0x0

    invoke-virtual {p0, v0, p1, p2}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translateTo(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .registers 5

    const/4 v0, 0x0

    invoke-virtual {p0, v0, p1, p2, p3}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translateTo(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;
    .registers 5

    const/4 v0, 0x0

    invoke-virtual {p0, v0, p1, p2, p3}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translateTo(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .registers 11

    const/4 v1, 0x0

    move-object v0, p0

    move-object v2, p1

    move-object v3, p2

    move-object v4, p3

    move-object v5, p4

    invoke-virtual/range {v0 .. v5}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translateTo(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;
    .registers 11

    const/4 v1, 0x0

    move-object v0, p0

    move-object v2, p1

    move-object v3, p2

    move-object v4, p3

    move-object v5, p4

    invoke-virtual/range {v0 .. v5}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translateTo(Ljava/lang/String;Ljava/io/File;)V
    .registers 4

    const/4 v0, 0x0

    invoke-virtual {p0, v0, p1, p2}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;)V

    return-void
.end method

.method public whitelist translateTo(Ljava/lang/String;Ljava/io/File;Ljava/lang/String;)V
    .registers 5

    const/4 v0, 0x0

    invoke-virtual {p0, v0, p1, p2, p3}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;Ljava/lang/String;)V

    return-void
.end method

.method public whitelist translateTo(Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;)V
    .registers 5

    const/4 v0, 0x0

    invoke-virtual {p0, v0, p1, p2, p3}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;)V

    return-void
.end method

.method public whitelist translateTo(Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;Ljava/lang/String;)V
    .registers 11

    const/4 v1, 0x0

    move-object v0, p0

    move-object v2, p1

    move-object v3, p2

    move-object v4, p3

    move-object v5, p4

    invoke-virtual/range {v0 .. v5}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;Ljava/lang/String;)V

    return-void
.end method

.method public whitelist translateTo(Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;[Ljava/lang/String;)V
    .registers 11

    const/4 v1, 0x0

    move-object v0, p0

    move-object v2, p1

    move-object v3, p2

    move-object v4, p3

    move-object v5, p4

    invoke-virtual/range {v0 .. v5}, Leu/xiaomi/util/JSONTranslator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;[Ljava/lang/String;[Ljava/lang/String;)V

    return-void
.end method
