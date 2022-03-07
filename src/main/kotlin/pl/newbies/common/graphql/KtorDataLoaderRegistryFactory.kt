package pl.newbies.common.graphql

import com.expediagroup.graphql.server.execution.DataLoaderRegistryFactory
import org.dataloader.DataLoaderRegistry
import pl.newbies.lecture.application.LectureSchema

class KtorDataLoaderRegistryFactory(
    private val lectureSchema: LectureSchema
) : DataLoaderRegistryFactory {

    override fun generate(): DataLoaderRegistry =
        DataLoaderRegistry().apply {
            register(lectureSchema.AuthorDataLoader().dataLoaderName, lectureSchema.AuthorDataLoader().getDataLoader())
            register(lectureSchema.IsFollowedDataLoader().dataLoaderName, lectureSchema.IsFollowedDataLoader().getDataLoader())
        }
}