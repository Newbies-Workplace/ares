package pl.newbies.event.domain.model

data class AddressDTO(
    var city: String,

    var place: String,

    var coordinates: CoordinatesDTO? = null,
)

data class CoordinatesDTO(
    var latitude: Double,

    var longitude: Double,
)