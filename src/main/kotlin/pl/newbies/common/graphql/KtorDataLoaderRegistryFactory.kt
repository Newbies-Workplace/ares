package pl.newbies.common.graphql

import com.expediagroup.graphql.server.execution.DataLoaderRegistryFactory
import org.dataloader.DataLoaderRegistry
import pl.newbies.event.application.EventSchema

class KtorDataLoaderRegistryFactory(
    private val eventSchema: EventSchema
) : DataLoaderRegistryFactory {

    override fun generate(): DataLoaderRegistry =
        DataLoaderRegistry().apply {
            register(eventSchema.AuthorDataLoader().dataLoaderName, eventSchema.AuthorDataLoader().getDataLoader())
            register(eventSchema.IsFollowedDataLoader().dataLoaderName, eventSchema.IsFollowedDataLoader().getDataLoader())
        }
}