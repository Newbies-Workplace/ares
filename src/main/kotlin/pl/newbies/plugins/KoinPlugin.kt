package pl.newbies.plugins

import io.ktor.events.EventDefinition
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.config.ApplicationConfig
import io.ktor.util.AttributeKey
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import org.koin.java.KoinJavaComponent.getKoin

//workaround until koin supports ktor 2.0
object KoinPlugin : ApplicationPlugin<Application, KoinApplication, Unit> {

    override val key: AttributeKey<Unit>
        get() = AttributeKey("Koin")

    override fun install(
        pipeline: Application,
        configure: KoinApplication.() -> Unit
    ) {
        val monitor = pipeline.environment.monitor
        val koinApplication = startKoin(appDeclaration = configure)
        monitor.raise(EventDefinition(), koinApplication)

        monitor.subscribe(ApplicationStopping) {
            monitor.raise(EventDefinition(), koinApplication)
            stopKoin()
            monitor.raise(EventDefinition(), koinApplication)
        }
    }
}

inline fun <reified T : Any> inject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
) = lazy { get<T>(qualifier, parameters) }


inline fun <reified T : Any> get(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
) = getKoin().get<T>(qualifier, parameters)

fun Scope.prop(key: String) =
    get<ApplicationConfig>().property(key)