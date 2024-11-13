-keepclasseswithmembernames class io.linksquared.Linksquared { *; }
-keep class io.linksquared.Linksquared { *; }
-keep class io.linksquared.Linksquared$** { *; }
-keepclasseswithmembernames class io.linksquared.Linksquared {
    public <methods>;
}
-keep class io.linksquared.Linksquared$Companion { *; }

-keepclassmembers class io.linksquared.Linksquared {
    public static ** Companion;
}

-keep class io.linksquared.model.** { *; }

-keep interface io.linksquared.LinksquaredDeeplinkListener {
   <methods>;
}
-keep interface io.linksquared.LinksquaredLinkGenerationListener {
   <methods>;
}
-keep interface io.linksquared.LinksquaredNotificationsListener {
   <methods>;
}