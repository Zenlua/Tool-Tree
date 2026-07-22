.class final Leu/xiaomi/util/Translator$SingletonHolder;
.super Ljava/lang/Object;
.source "Translator.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Leu/xiaomi/util/Translator;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1a
    name = "SingletonHolder"
.end annotation


# static fields
.field private static final blacklist INSTANCE:Leu/xiaomi/util/Translator;


# direct methods
.method static bridge synthetic blacklist -$$Nest$sfgetINSTANCE()Leu/xiaomi/util/Translator;
    .registers 1

    sget-object v0, Leu/xiaomi/util/Translator$SingletonHolder;->INSTANCE:Leu/xiaomi/util/Translator;

    return-object v0
.end method

.method static constructor blacklist <clinit>()V
    .registers 2

    new-instance v0, Leu/xiaomi/util/Translator;

    const/4 v1, 0x0

    invoke-direct {v0, v1}, Leu/xiaomi/util/Translator;-><init>(Leu/xiaomi/util/Translator-IA;)V

    sput-object v0, Leu/xiaomi/util/Translator$SingletonHolder;->INSTANCE:Leu/xiaomi/util/Translator;

    return-void
.end method

.method private constructor blacklist <init>()V
    .registers 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method
