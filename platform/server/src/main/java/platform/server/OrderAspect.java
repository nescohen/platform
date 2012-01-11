package platform.server;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclarePrecedence;


@Aspect
@DeclarePrecedence("platform.server.caches.CacheAspect, platform.server.data.translator.AfterTranslateAspect, platform.server.caches.AutoHintsAspect, platform.server.data.query.MapCacheAspect, platform.server.caches.WrapComplexityAspect, platform.server.MessageAspect")
public class OrderAspect {
}
