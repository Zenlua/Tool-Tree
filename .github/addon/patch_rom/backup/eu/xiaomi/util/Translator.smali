.class public final Leu/xiaomi/util/Translator;
.super Ljava/lang/Object;
.source "Translator.java"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Leu/xiaomi/util/Translator$SingletonHolder;,
        Leu/xiaomi/util/Translator$State;
    }
.end annotation


# static fields
.field private static final blacklist BUFFER_SIZE:I = 0x1000

.field private static final blacklist CHARSET:Ljava/lang/String; = "UTF-8"

.field private static final blacklist CLIENT_TYPE:Ljava/lang/String; = "at"

.field private static final blacklist CLIENT_VERSION:D = 1.0

.field private static final blacklist CONN_TIMEOUT:I = 0x4e20

.field private static final blacklist LANG_AUTO:Ljava/lang/String; = "auto"

.field private static final blacklist QUERY_MAX_LEN:I = 0x2710

.field private static final blacklist TAG:Ljava/lang/String; = "Translator"

.field private static final blacklist USER_AGENT:Ljava/lang/String;


# direct methods
.method static constructor blacklist <clinit>()V
    .registers 2

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "GoogleTranslate/5.22.0.RC04.206832067 {Linux; U; Android "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    sget-object v1, Landroid/os/Build$VERSION;->RELEASE:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    const-string v1, "; "

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    sget-object v1, Landroid/os/Build;->MODEL:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    const-string v1, ")"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    sput-object v0, Leu/xiaomi/util/Translator;->USER_AGENT:Ljava/lang/String;

    return-void
.end method

.method private constructor blacklist <init>()V
    .registers 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method synthetic constructor blacklist <init>(Leu/xiaomi/util/Translator-IA;)V
    .registers 2

    invoke-direct {p0}, Leu/xiaomi/util/Translator;-><init>()V

    return-void
.end method

.method public static whitelist getInstance()Leu/xiaomi/util/Translator;
    .registers 1

    invoke-static {}, Leu/xiaomi/util/Translator$SingletonHolder;->-$$Nest$sfgetINSTANCE()Leu/xiaomi/util/Translator;

    move-result-object v0

    return-object v0
.end method

.method private static blacklist toProperCase(Ljava/lang/String;)Ljava/lang/String;
    .registers 11

    if-nez p0, :cond_4

    const/4 p0, 0x0

    return-object p0

    :cond_4
    invoke-virtual {p0}, Ljava/lang/String;->length()I

    move-result v0

    if-nez v0, :cond_b

    return-object p0

    :cond_b
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const/4 v2, 0x0

    move v3, v2

    :goto_12
    if-ge v3, v0, :cond_2e

    invoke-virtual {p0, v3}, Ljava/lang/String;->charAt(I)C

    move-result v4

    invoke-static {v4}, Ljava/lang/Character;->isAlphabetic(I)Z

    move-result v5

    if-eqz v5, :cond_28

    invoke-static {v4}, Ljava/lang/Character;->toUpperCase(C)C

    move-result v4

    invoke-virtual {v1, v4}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    add-int/lit8 v3, v3, 0x1

    goto :goto_2e

    :cond_28
    invoke-virtual {v1, v4}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    add-int/lit8 v3, v3, 0x1

    goto :goto_12

    :cond_2e
    :goto_2e
    const/4 v4, 0x3

    new-array v5, v4, [C

    fill-array-data v5, :array_62

    move v6, v2

    :goto_35
    if-ge v3, v0, :cond_5c

    invoke-virtual {p0, v3}, Ljava/lang/String;->charAt(I)C

    move-result v7

    if-eqz v6, :cond_4a

    const/16 v8, 0x20

    if-eq v7, v8, :cond_4a

    invoke-static {v7}, Ljava/lang/Character;->toUpperCase(C)C

    move-result v6

    invoke-virtual {v1, v6}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    move v6, v2

    goto :goto_4d

    :cond_4a
    invoke-virtual {v1, v7}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    :goto_4d
    move v8, v2

    :goto_4e
    if-ge v8, v4, :cond_59

    aget-char v9, v5, v8

    if-ne v7, v9, :cond_56

    const/4 v6, 0x1

    goto :goto_59

    :cond_56
    add-int/lit8 v8, v8, 0x1

    goto :goto_4e

    :cond_59
    :goto_59
    add-int/lit8 v3, v3, 0x1

    goto :goto_35

    :cond_5c
    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    return-object p0

    nop

    :array_62
    .array-data 2
        0x2es
        0x3fs
        0x21s
    .end array-data
.end method


# virtual methods
.method public whitelist translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .registers 6

    invoke-static {p3}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v0

    if-eqz v0, :cond_7

    return-object p3

    :cond_7
    const/4 v0, 0x1

    new-array v0, v0, [Ljava/lang/String;

    const/4 v1, 0x0

    aput-object p3, v0, v1

    invoke-virtual {p0, p1, p2, v0}, Leu/xiaomi/util/Translator;->translate(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)V

    aget-object p1, v0, v1

    return-object p1
.end method

.method blacklist translate(Ljava/lang/String;Ljava/lang/String;Leu/xiaomi/util/Translator$State;)V
    .registers 20
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;,
            Lorg/json/JSONException;
        }
    .end annotation

    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;

    move-result-object v0

    invoke-virtual {v0}, Landroid/os/Looper;->isCurrentThread()Z

    move-result v0

    if-eqz v0, :cond_12

    const-string v0, "Translator"

    const-string v1, "Cannot run translator on UI thread."

    invoke-static {v0, v1}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    return-void

    :cond_12
    move-object/from16 v0, p3

    iget-object v0, v0, Leu/xiaomi/util/Translator$State;->textList:Ljava/util/List;

    invoke-interface {v0}, Ljava/util/List;->size()I

    move-result v1

    if-nez v1, :cond_1d

    return-void

    :cond_1d
    const/4 v2, 0x1

    if-ne v1, v2, :cond_27

    const-string v3, ""

    invoke-interface {v0, v3}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    add-int/lit8 v1, v1, 0x1

    :cond_27
    invoke-static/range {p1 .. p1}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v3

    const-string v4, "auto"

    if-eqz v3, :cond_31

    move-object v3, v4

    goto :goto_33

    :cond_31
    move-object/from16 v3, p1

    :goto_33
    invoke-static/range {p2 .. p2}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v5

    if-eqz v5, :cond_3a

    goto :goto_3c

    :cond_3a
    move-object/from16 v4, p2

    :goto_3c
    invoke-static {}, Ljava/util/Locale;->getDefault()Ljava/util/Locale;

    move-result-object v5

    invoke-virtual {v5}, Ljava/util/Locale;->getLanguage()Ljava/lang/String;

    move-result-object v5

    new-instance v6, Ljava/lang/StringBuilder;

    invoke-direct {v6}, Ljava/lang/StringBuilder;-><init>()V

    const/4 v7, 0x0

    move v8, v7

    :goto_4b
    if-ge v8, v1, :cond_162

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->setLength(I)V

    const-string v9, "q="

    invoke-virtual {v6, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-interface {v0, v8}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v9

    check-cast v9, Ljava/lang/String;

    const-string v10, "UTF-8"

    invoke-static {v9, v10}, Ljava/net/URLEncoder;->encode(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v9

    invoke-virtual {v6, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move v9, v8

    :goto_65
    add-int/lit8 v11, v9, 0x1

    const-string v12, "&q="

    if-ge v11, v1, :cond_8c

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->length()I

    move-result v13

    invoke-interface {v0, v11}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v14

    check-cast v14, Ljava/lang/String;

    invoke-static {v14, v10}, Ljava/net/URLEncoder;->encode(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v14

    invoke-virtual {v14}, Ljava/lang/String;->length()I

    move-result v15

    add-int/2addr v13, v15

    add-int/lit8 v13, v13, 0x3

    const/16 v15, 0x2710

    if-gt v13, v15, :cond_8c

    invoke-virtual {v6, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v6, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move v9, v11

    goto :goto_65

    :cond_8c
    sub-int/2addr v9, v8

    add-int/lit8 v13, v9, 0x1

    if-ne v13, v2, :cond_96

    invoke-virtual {v6, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    add-int/lit8 v13, v9, 0x2

    :cond_96
    new-instance v9, Ljava/net/URL;

    new-instance v12, Ljava/lang/StringBuilder;

    const-string v14, "https://translate.google.com/translate_a/t?"

    invoke-direct {v12, v14}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v12, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object v12

    const-string v14, "&sl="

    invoke-virtual {v12, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v12

    invoke-virtual {v12, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v12

    const-string v14, "&tl="

    invoke-virtual {v12, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v12

    invoke-virtual {v12, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v12

    const-string v14, "&hl="

    invoke-virtual {v12, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v12

    invoke-virtual {v12, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v12

    const-string v14, "&ie=UTF-8&oe=UTF-8&client=at&v=1.0"

    invoke-virtual {v12, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v12

    invoke-virtual {v12}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v12

    invoke-direct {v9, v12}, Ljava/net/URL;-><init>(Ljava/lang/String;)V

    invoke-virtual {v9}, Ljava/net/URL;->openConnection()Ljava/net/URLConnection;

    move-result-object v9

    check-cast v9, Ljava/net/HttpURLConnection;

    const/16 v12, 0x4e20

    invoke-virtual {v9, v12}, Ljava/net/HttpURLConnection;->setConnectTimeout(I)V

    invoke-virtual {v9, v12}, Ljava/net/HttpURLConnection;->setReadTimeout(I)V

    const-string v12, "User-Agent"

    sget-object v14, Leu/xiaomi/util/Translator;->USER_AGENT:Ljava/lang/String;

    invoke-virtual {v9, v12, v14}, Ljava/net/HttpURLConnection;->setRequestProperty(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v9}, Ljava/net/HttpURLConnection;->connect()V

    const/16 v12, 0x1000

    new-array v14, v12, [B

    new-instance v15, Ljava/io/ByteArrayOutputStream;

    invoke-direct {v15}, Ljava/io/ByteArrayOutputStream;-><init>()V

    :try_start_ef
    new-instance v2, Ljava/io/BufferedInputStream;

    invoke-virtual {v9}, Ljava/net/HttpURLConnection;->getInputStream()Ljava/io/InputStream;

    move-result-object v9

    invoke-direct {v2, v9, v12}, Ljava/io/BufferedInputStream;-><init>(Ljava/io/InputStream;I)V
    :try_end_f8
    .catchall {:try_start_ef .. :try_end_f8} :catchall_156

    :goto_f8
    :try_start_f8
    invoke-virtual {v2, v14}, Ljava/io/BufferedInputStream;->read([B)I

    move-result v9

    const/4 v12, -0x1

    if-eq v9, v12, :cond_103

    invoke-virtual {v15, v14, v7, v9}, Ljava/io/ByteArrayOutputStream;->write([BII)V

    goto :goto_f8

    :cond_103
    invoke-virtual {v15, v10}, Ljava/io/ByteArrayOutputStream;->toString(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v9
    :try_end_107
    .catchall {:try_start_f8 .. :try_end_107} :catchall_14a

    :try_start_107
    invoke-virtual {v2}, Ljava/io/BufferedInputStream;->close()V
    :try_end_10a
    .catchall {:try_start_107 .. :try_end_10a} :catchall_156

    invoke-virtual {v15}, Ljava/io/ByteArrayOutputStream;->close()V

    invoke-static {v9}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v2

    if-eqz v2, :cond_114

    return-void

    :cond_114
    new-instance v2, Lorg/json/JSONArray;

    invoke-direct {v2, v9}, Lorg/json/JSONArray;-><init>(Ljava/lang/String;)V

    invoke-virtual {v2}, Lorg/json/JSONArray;->length()I

    move-result v9

    if-eq v9, v13, :cond_120

    return-void

    :cond_120
    move v10, v7

    :goto_121
    if-ge v10, v9, :cond_146

    invoke-virtual {v2, v10}, Lorg/json/JSONArray;->get(I)Ljava/lang/Object;

    move-result-object v12

    instance-of v13, v12, Lorg/json/JSONArray;

    if-eqz v13, :cond_132

    check-cast v12, Lorg/json/JSONArray;

    invoke-virtual {v12, v7}, Lorg/json/JSONArray;->getString(I)Ljava/lang/String;

    move-result-object v12

    goto :goto_134

    :cond_132
    check-cast v12, Ljava/lang/String;

    :goto_134
    invoke-static {v12}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v13

    if-nez v13, :cond_143

    add-int v13, v8, v10

    invoke-static {v12}, Leu/xiaomi/util/Translator;->toProperCase(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v12

    invoke-interface {v0, v13, v12}, Ljava/util/List;->set(ILjava/lang/Object;)Ljava/lang/Object;

    :cond_143
    add-int/lit8 v10, v10, 0x1

    goto :goto_121

    :cond_146
    move v8, v11

    const/4 v2, 0x1

    goto/16 :goto_4b

    :catchall_14a
    move-exception v0

    move-object v1, v0

    :try_start_14c
    invoke-virtual {v2}, Ljava/io/BufferedInputStream;->close()V
    :try_end_14f
    .catchall {:try_start_14c .. :try_end_14f} :catchall_150

    goto :goto_155

    :catchall_150
    move-exception v0

    move-object v2, v0

    :try_start_152
    invoke-virtual {v1, v2}, Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V

    :goto_155
    throw v1
    :try_end_156
    .catchall {:try_start_152 .. :try_end_156} :catchall_156

    :catchall_156
    move-exception v0

    move-object v1, v0

    :try_start_158
    invoke-virtual {v15}, Ljava/io/ByteArrayOutputStream;->close()V
    :try_end_15b
    .catchall {:try_start_158 .. :try_end_15b} :catchall_15c

    goto :goto_161

    :catchall_15c
    move-exception v0

    move-object v2, v0

    invoke-virtual {v1, v2}, Ljava/lang/Throwable;->addSuppressed(Ljava/lang/Throwable;)V

    :goto_161
    throw v1

    :cond_162
    return-void
.end method

.method public whitelist translate(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)V
    .registers 9

    if-eqz p3, :cond_4d

    array-length v0, p3

    if-nez v0, :cond_6

    goto :goto_4d

    :cond_6
    new-instance v0, Leu/xiaomi/util/Translator$State;

    invoke-direct {v0}, Leu/xiaomi/util/Translator$State;-><init>()V

    const/4 v1, 0x0

    :goto_c
    array-length v2, p3

    if-ge v1, v2, :cond_3f

    aget-object v2, p3, v1

    invoke-virtual {v2}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v2

    iget-object v3, v0, Leu/xiaomi/util/Translator$State;->textSet:Ljava/util/Set;

    invoke-interface {v3, v2}, Ljava/util/Set;->contains(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_27

    iget-object v3, v0, Leu/xiaomi/util/Translator$State;->textList:Ljava/util/List;

    invoke-interface {v3, v2}, Ljava/util/List;->indexOf(Ljava/lang/Object;)I

    move-result v2

    move v4, v2

    move v2, v1

    move v1, v4

    goto :goto_33

    :cond_27
    iget-object v3, v0, Leu/xiaomi/util/Translator$State;->textSet:Ljava/util/Set;

    invoke-interface {v3, v2}, Ljava/util/Set;->add(Ljava/lang/Object;)Z

    iget-object v3, v0, Leu/xiaomi/util/Translator$State;->textList:Ljava/util/List;

    invoke-interface {v3, v2}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    add-int/lit8 v2, v1, 0x1

    :goto_33
    iget-object v3, v0, Leu/xiaomi/util/Translator$State;->indexList:Ljava/util/List;

    invoke-static {v1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v1

    invoke-interface {v3, v1}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    add-int/lit8 v1, v2, 0x1

    goto :goto_c

    :cond_3f
    :try_start_3f
    invoke-virtual {p0, p1, p2, v0}, Leu/xiaomi/util/Translator;->translate(Ljava/lang/String;Ljava/lang/String;Leu/xiaomi/util/Translator$State;)V
    :try_end_42
    .catch Ljava/lang/Exception; {:try_start_3f .. :try_end_42} :catch_43

    goto :goto_4d

    :catch_43
    move-exception p1

    const-string p2, "Translator"

    invoke-virtual {p1}, Ljava/lang/Exception;->getMessage()Ljava/lang/String;

    move-result-object p3

    invoke-static {p2, p3, p1}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    :cond_4d
    :goto_4d
    return-void
.end method

.method public whitelist translateAuto(Ljava/lang/String;)Ljava/lang/String;
    .registers 3

    const/4 v0, 0x0

    invoke-virtual {p0, v0, v0, p1}, Leu/xiaomi/util/Translator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translateAuto([Ljava/lang/String;)V
    .registers 3

    const/4 v0, 0x0

    invoke-virtual {p0, v0, v0, p1}, Leu/xiaomi/util/Translator;->translate(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)V

    return-void
.end method

.method public whitelist translateFrom(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .registers 4

    const/4 v0, 0x0

    invoke-virtual {p0, p1, v0, p2}, Leu/xiaomi/util/Translator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translateFrom(Ljava/lang/String;[Ljava/lang/String;)V
    .registers 4

    const/4 v0, 0x0

    invoke-virtual {p0, p1, v0, p2}, Leu/xiaomi/util/Translator;->translate(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)V

    return-void
.end method

.method public whitelist translateTo(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .registers 4

    const/4 v0, 0x0

    invoke-virtual {p0, v0, p1, p2}, Leu/xiaomi/util/Translator;->translate(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    return-object p1
.end method

.method public whitelist translateTo(Ljava/lang/String;[Ljava/lang/String;)V
    .registers 4

    const/4 v0, 0x0

    invoke-virtual {p0, v0, p1, p2}, Leu/xiaomi/util/Translator;->translate(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)V

    return-void
.end method
