package com.ridelink.drivervehicle;

import tools.jackson.databind.ObjectMapper;
import com.ridelink.drivervehicle.dto.request.AvailabilityUpdateDto;
import com.ridelink.drivervehicle.dto.request.DriverRequestDto;
import com.ridelink.drivervehicle.dto.request.LocationUpdateDto;
import com.ridelink.drivervehicle.dto.request.VehicleRequestDto;
import com.ridelink.drivervehicle.entity.AvailabilityStatus;
import com.ridelink.drivervehicle.repository.DriverRepository;
import com.ridelink.drivervehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class DriverVehicleServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    /** Deterministic Account Service UUIDs for test drivers, e.g. acc(101). */
    private static UUID acc(int n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    @BeforeEach
    void setUp() {
        vehicleRepository.deleteAll();
        driverRepository.deleteAll();
    }

    @Test
    @DisplayName("Should start with empty database without any seed or hardcoded records")
    void shouldStartWithEmptyDatabase() throws Exception {
        mockMvc.perform(get("/api/drivers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should register new driver with HTTP 201 and accountId reference")
    void shouldRegisterNewDriver() throws Exception {
        DriverRequestDto request = DriverRequestDto.builder()
                .accountId(acc(101))
                .name("Kamal Perera")
                .phone("+94771234567")
                .email("kamal.perera@ridelink.com")
                .licenseNo("DL-1001-WP")
                .availability(AvailabilityStatus.AVAILABLE)
                .serviceArea("Colombo")
                .latitude(6.9271)
                .longitude(79.8612)
                .build();

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.accountId", is(acc(101).toString())))
                .andExpect(jsonPath("$.name", is("Kamal Perera")))
                .andExpect(jsonPath("$.phone", is("+94771234567")))
                .andExpect(jsonPath("$.licenseNo", is("DL-1001-WP")))
                .andExpect(jsonPath("$.availability", is("AVAILABLE")))
                .andExpect(jsonPath("$.serviceArea", is("Colombo")))
                .andExpect(jsonPath("$.latitude", is(6.9271)))
                .andExpect(jsonPath("$.longitude", is(79.8612)))
                .andExpect(jsonPath("$.locationUpdatedAt", notNullValue()));
    }

    @Test
    @DisplayName("Should return 409 Conflict when registering driver with duplicate license")
    void shouldReturnConflictOnDuplicateLicense() throws Exception {
        DriverRequestDto request = DriverRequestDto.builder()
                .accountId(acc(101))
                .name("Kamal Perera")
                .phone("+94771234567")
                .email("kamal.perera@ridelink.com")
                .licenseNo("DL-1001-WP")
                .availability(AvailabilityStatus.AVAILABLE)
                .build();

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Attempt duplicate
        DriverRequestDto duplicateRequest = DriverRequestDto.builder()
                .accountId(acc(102))
                .name("Another Kamal")
                .phone("+94779999999")
                .email("kamal2@ridelink.com")
                .licenseNo("DL-1001-WP")
                .availability(AvailabilityStatus.AVAILABLE)
                .build();

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when mandatory fields are missing")
    void shouldReturnBadRequestOnMissingFields() throws Exception {
        DriverRequestDto request = DriverRequestDto.builder()
                .name("")
                .phone("")
                .licenseNo("")
                .build();

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.name", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.phone", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.licenseNo", notNullValue()));
    }

    @Test
    @DisplayName("Should update driver operational profile with PUT /api/drivers/{id}")
    void shouldUpdateDriverProfile() throws Exception {
        DriverRequestDto createReq = DriverRequestDto.builder()
                .accountId(acc(101))
                .name("Kamal Perera")
                .phone("+94771234567")
                .licenseNo("DL-1001-WP")
                .availability(AvailabilityStatus.AVAILABLE)
                .serviceArea("Colombo")
                .build();

        MvcResult result = mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long driverId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        DriverRequestDto updateReq = DriverRequestDto.builder()
                .accountId(acc(101))
                .name("Kamal P. Updated")
                .phone("+94779876543")
                .licenseNo("DL-1001-WP")
                .availability(AvailabilityStatus.BUSY)
                .serviceArea("Kandy")
                .latitude(7.2906)
                .longitude(80.6337)
                .build();

        mockMvc.perform(put("/api/drivers/" + driverId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Kamal P. Updated")))
                .andExpect(jsonPath("$.phone", is("+94779876543")))
                .andExpect(jsonPath("$.availability", is("BUSY")))
                .andExpect(jsonPath("$.serviceArea", is("Kandy")))
                .andExpect(jsonPath("$.latitude", is(7.2906)))
                .andExpect(jsonPath("$.longitude", is(80.6337)));
    }

    @Test
    @DisplayName("Should update driver availability status to AVAILABLE, BUSY, OFFLINE")
    void shouldUpdateAvailabilityStatuses() throws Exception {
        DriverRequestDto driver = DriverRequestDto.builder()
                .name("Kamal Perera")
                .phone("+94771234567")
                .licenseNo("DL-1001-WP")
                .availability(AvailabilityStatus.AVAILABLE)
                .build();

        MvcResult result = mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(driver)))
                .andExpect(status().isCreated())
                .andReturn();

        Long driverId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // 1. Set BUSY via PUT
        AvailabilityUpdateDto busyDto = AvailabilityUpdateDto.builder().availability(AvailabilityStatus.BUSY).build();
        mockMvc.perform(put("/api/drivers/" + driverId + "/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(busyDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability", is("BUSY")));

        // 2. Set OFFLINE via PUT
        AvailabilityUpdateDto offlineDto = AvailabilityUpdateDto.builder().availability(AvailabilityStatus.OFFLINE).build();
        mockMvc.perform(put("/api/drivers/" + driverId + "/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offlineDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability", is("OFFLINE")));

        // 3. Set AVAILABLE via PUT
        AvailabilityUpdateDto availDto = AvailabilityUpdateDto.builder().availability(AvailabilityStatus.AVAILABLE).build();
        mockMvc.perform(put("/api/drivers/" + driverId + "/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(availDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability", is("AVAILABLE")));
    }

    @Test
    @DisplayName("Should update simulated driver location with PUT /api/drivers/{id}/location")
    void shouldUpdateSimulatedLocation() throws Exception {
        DriverRequestDto driver = DriverRequestDto.builder()
                .name("Kamal Perera")
                .phone("+94771234567")
                .licenseNo("DL-1001-WP")
                .availability(AvailabilityStatus.AVAILABLE)
                .build();

        MvcResult result = mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(driver)))
                .andExpect(status().isCreated())
                .andReturn();

        Long driverId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        LocationUpdateDto locDto = LocationUpdateDto.builder()
                .latitude(6.9271)
                .longitude(79.8612)
                .build();

        mockMvc.perform(put("/api/drivers/" + driverId + "/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(driverId.intValue())))
                .andExpect(jsonPath("$.latitude", is(6.9271)))
                .andExpect(jsonPath("$.longitude", is(79.8612)))
                .andExpect(jsonPath("$.locationUpdatedAt", notNullValue()));
    }

    @Test
    @DisplayName("Should create vehicle, assign to driver, update and retrieve")
    void shouldManageVehiclesAndAssignment() throws Exception {
        // Create driver
        DriverRequestDto driver = DriverRequestDto.builder()
                .accountId(acc(101))
                .name("Kamal Perera")
                .phone("+94771234567")
                .licenseNo("DL-1001-WP")
                .availability(AvailabilityStatus.AVAILABLE)
                .build();

        MvcResult dResult = mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(driver)))
                .andExpect(status().isCreated())
                .andReturn();

        Long driverId = objectMapper.readTree(dResult.getResponse().getContentAsString()).get("id").asLong();

        // Create vehicle unassigned initially or with driverId
        VehicleRequestDto vReq = VehicleRequestDto.builder()
                .driverId(driverId)
                .vehicleType("CAR")
                .model("Toyota Prius")
                .plateNumber("CAB-1234")
                .build();

        MvcResult vResult = mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.driverId", is(driverId.intValue())))
                .andExpect(jsonPath("$.vehicleType", is("CAR")))
                .andExpect(jsonPath("$.model", is("Toyota Prius")))
                .andExpect(jsonPath("$.plateNumber", is("CAB-1234")))
                .andExpect(jsonPath("$.vehicleNumber", is("CAB-1234")))
                .andReturn();

        Long vehicleId = objectMapper.readTree(vResult.getResponse().getContentAsString()).get("id").asLong();

        // Get vehicle by ID
        mockMvc.perform(get("/api/vehicles/" + vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(vehicleId.intValue())));

        // Get vehicles by driver ID
        mockMvc.perform(get("/api/vehicles/driver/" + driverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].plateNumber", is("CAB-1234")));

        // Update vehicle
        VehicleRequestDto updateVReq = VehicleRequestDto.builder()
                .driverId(driverId)
                .vehicleType("VAN")
                .model("Toyota KDH")
                .plateNumber("CAB-1234")
                .build();

        mockMvc.perform(put("/api/vehicles/" + vehicleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateVReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleType", is("VAN")))
                .andExpect(jsonPath("$.model", is("Toyota KDH")));
    }

    @Test
    @DisplayName("Should retrieve eligible available drivers for Ride Management Service (Member 3)")
    void shouldRetrieveEligibleAvailableDriversForRideManagement() throws Exception {
        // Driver 1: Colombo, AVAILABLE, with vehicle CAB-1234 -> ELIGIBLE
        DriverRequestDto d1 = DriverRequestDto.builder()
                .accountId(acc(101))
                .name("Kamal Perera")
                .phone("+94771234567")
                .licenseNo("DL-1001-WP")
                .availability(AvailabilityStatus.AVAILABLE)
                .serviceArea("Colombo")
                .latitude(6.9271)
                .longitude(79.8612)
                .build();
        MvcResult r1 = mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(d1)))
                .andExpect(status().isCreated())
                .andReturn();
        Long d1Id = objectMapper.readTree(r1.getResponse().getContentAsString()).get("id").asLong();

        VehicleRequestDto v1 = VehicleRequestDto.builder()
                .driverId(d1Id)
                .vehicleType("CAR")
                .model("Toyota Prius")
                .plateNumber("CAB-1234")
                .build();
        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(v1)))
                .andExpect(status().isCreated());

        // Driver 2: Colombo, BUSY, with vehicle -> NOT ELIGIBLE (because BUSY)
        DriverRequestDto d2 = DriverRequestDto.builder()
                .accountId(acc(102))
                .name("Nimal Fernando")
                .phone("+94772345678")
                .licenseNo("DL-1002-WP")
                .availability(AvailabilityStatus.BUSY)
                .serviceArea("Colombo")
                .latitude(6.9280)
                .longitude(79.8620)
                .build();
        MvcResult r2 = mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(d2)))
                .andExpect(status().isCreated())
                .andReturn();
        Long d2Id = objectMapper.readTree(r2.getResponse().getContentAsString()).get("id").asLong();

        VehicleRequestDto v2 = VehicleRequestDto.builder()
                .driverId(d2Id)
                .vehicleType("CAR")
                .model("Honda Civic")
                .plateNumber("WP-CAR-5678")
                .build();
        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(v2)))
                .andExpect(status().isCreated());

        // Driver 3: Colombo, AVAILABLE, BUT NO VEHICLE -> NOT ELIGIBLE
        DriverRequestDto d3 = DriverRequestDto.builder()
                .accountId(acc(103))
                .name("Sunil Perera")
                .phone("+94773456789")
                .licenseNo("DL-1003-WP")
                .availability(AvailabilityStatus.AVAILABLE)
                .serviceArea("Colombo")
                .build();
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(d3)))
                .andExpect(status().isCreated());

        // Driver 4: Kandy, AVAILABLE, with vehicle -> NOT ELIGIBLE for Colombo (different area)
        DriverRequestDto d4 = DriverRequestDto.builder()
                .accountId(acc(104))
                .name("Ravi Shanmugam")
                .phone("+94774567890")
                .licenseNo("DL-1004-CP")
                .availability(AvailabilityStatus.AVAILABLE)
                .serviceArea("Kandy")
                .latitude(7.2906)
                .longitude(80.6337)
                .build();
        MvcResult r4 = mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(d4)))
                .andExpect(status().isCreated())
                .andReturn();
        Long d4Id = objectMapper.readTree(r4.getResponse().getContentAsString()).get("id").asLong();

        VehicleRequestDto v4 = VehicleRequestDto.builder()
                .driverId(d4Id)
                .vehicleType("VAN")
                .model("Toyota HiAce")
                .plateNumber("CP-VAN-9012")
                .build();
        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(v4)))
                .andExpect(status().isCreated());

        // Test GET /api/drivers/eligible?serviceArea=Colombo
        mockMvc.perform(get("/api/drivers/eligible")
                        .param("serviceArea", "Colombo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].driverId", is(d1Id.intValue())))
                .andExpect(jsonPath("$[0].accountId", is(acc(101).toString())))
                .andExpect(jsonPath("$[0].driverName", is("Kamal Perera")))
                .andExpect(jsonPath("$[0].vehicleNumber", is("CAB-1234")))
                .andExpect(jsonPath("$[0].vehicleType", is("CAR")))
                .andExpect(jsonPath("$[0].availability", is("AVAILABLE")))
                .andExpect(jsonPath("$[0].serviceArea", is("Colombo")))
                .andExpect(jsonPath("$[0].latitude", is(6.9271)))
                .andExpect(jsonPath("$[0].longitude", is(79.8612)));

        // Test GET /api/drivers/eligible?serviceArea=Kandy
        mockMvc.perform(get("/api/drivers/eligible")
                        .param("serviceArea", "Kandy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].driverId", is(d4Id.intValue())))
                .andExpect(jsonPath("$[0].driverName", is("Ravi Shanmugam")))
                .andExpect(jsonPath("$[0].vehicleNumber", is("CP-VAN-9012")));
    }

    @Test
    @DisplayName("Should delete driver and cascade delete vehicle")
    void shouldDeleteDriverAndCascade() throws Exception {
        DriverRequestDto driver = DriverRequestDto.builder()
                .name("Kamal Perera")
                .phone("+94771234567")
                .licenseNo("DL-1001-WP")
                .availability(AvailabilityStatus.AVAILABLE)
                .build();

        MvcResult dRes = mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(driver)))
                .andExpect(status().isCreated())
                .andReturn();

        Long driverId = objectMapper.readTree(dRes.getResponse().getContentAsString()).get("id").asLong();

        VehicleRequestDto vReq = VehicleRequestDto.builder()
                .driverId(driverId)
                .vehicleType("CAR")
                .model("Toyota Prius")
                .plateNumber("CAB-1234")
                .build();
        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vReq)))
                .andExpect(status().isCreated());

        // Delete Driver
        mockMvc.perform(delete("/api/drivers/" + driverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Verify driver not found
        mockMvc.perform(get("/api/drivers/" + driverId))
                .andExpect(status().isNotFound());

        // Verify vehicles for driver is empty
        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should return nearby available drivers in Ride Management Service format")
    void shouldReturnNearbyAvailableDriversForRideManagement() throws Exception {
        // Near (~1 km), AVAILABLE, linked account -> included, nearest
        createDriver(acc(201), "Near Driver", "DL-2001-WP", AvailabilityStatus.AVAILABLE, 6.9300, 79.8612);
        // ~3 km, AVAILABLE, linked account -> included, second
        createDriver(acc(202), "Mid Driver", "DL-2002-WP", AvailabilityStatus.AVAILABLE, 6.9541, 79.8612);
        // Near but BUSY -> excluded
        createDriver(acc(203), "Busy Driver", "DL-2003-WP", AvailabilityStatus.BUSY, 6.9280, 79.8612);
        // Near but no account link -> excluded (Ride service needs an account id)
        createDriver(null, "Unlinked Driver", "DL-2004-WP", AvailabilityStatus.AVAILABLE, 6.9275, 79.8612);
        // Kandy (~95 km) -> outside radius
        createDriver(acc(205), "Far Driver", "DL-2005-CP", AvailabilityStatus.AVAILABLE, 7.2906, 80.6337);

        mockMvc.perform(get("/api/drivers/available")
                        .param("lat", "6.9210")
                        .param("lng", "79.8612")
                        .param("radius", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(acc(201).toString())))
                .andExpect(jsonPath("$[0].lat", is(6.93)))
                .andExpect(jsonPath("$[0].lng", is(79.8612)))
                .andExpect(jsonPath("$[0].serviceArea", is("Colombo")))
                .andExpect(jsonPath("$[1].id", is(acc(202).toString())));

        // Without lat/lng the original endpoint is unchanged (all AVAILABLE drivers, full profile)
        mockMvc.perform(get("/api/drivers/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].licenseNo").exists());
    }

    private void createDriver(UUID accountId, String name, String licenseNo,
                              AvailabilityStatus availability, double lat, double lng) throws Exception {
        DriverRequestDto request = DriverRequestDto.builder()
                .accountId(accountId)
                .name(name)
                .phone("+94770000000")
                .licenseNo(licenseNo)
                .availability(availability)
                .serviceArea("Colombo")
                .latitude(lat)
                .longitude(lng)
                .build();
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
