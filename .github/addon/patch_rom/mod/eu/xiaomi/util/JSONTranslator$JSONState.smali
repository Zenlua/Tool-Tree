.class final Leu/xiaomi/util/JSONTranslator$JSONState;
.super Leu/xiaomi/util/Translator$State;
.source "JSONTranslator.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Leu/xiaomi/util/JSONTranslator;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1a
    name = "JSONState"
.end annotation


# instance fields
.field final blacklist delimPattern:[Ljava/util/regex/Pattern;

.field final blacklist keyList:Ljava/util/List;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List<",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation
.end field


# direct methods
.method constructor blacklist <init>([Ljava/lang/String;[Ljava/util/regex/Pattern;)V
    .registers 3

    invoke-direct {p0}, Leu/xiaomi/util/Translator$State;-><init>()V

    if-eqz p1, :cond_a

    invoke-static {p1}, Ljava/util/Arrays;->asList([Ljava/lang/Object;)Ljava/util/List;

    move-result-object p1

    goto :goto_b

    :cond_a
    const/4 p1, 0x0

    :goto_b
    iput-object p1, p0, Leu/xiaomi/util/JSONTranslator$JSONState;->keyList:Ljava/util/List;

    iput-object p2, p0, Leu/xiaomi/util/JSONTranslator$JSONState;->delimPattern:[Ljava/util/regex/Pattern;

    return-void
.end method
