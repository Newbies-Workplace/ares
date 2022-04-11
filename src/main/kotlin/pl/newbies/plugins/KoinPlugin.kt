package pl.newbies.plugins

import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.config.ApplicationConfig
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin

// workaround for https://github.com/InsertKoinIO/koin/pull/1266
val KoinPlugin : ApplicationPlugin<KoinApplication> =
    createApplicationPlugin("Koin", { KoinApplication.init() }) {

        val monitor = application.environment.monitor

        startKoin(koinApplication = this.pluginConfig)

        monitor.subscribe(ApplicationStopping) { stopKoin() }
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

fun configModule(config: ApplicationConfig) = module {
    single { config }
}