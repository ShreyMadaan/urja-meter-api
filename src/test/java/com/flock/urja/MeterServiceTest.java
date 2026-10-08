package com.flock.urja;
import tools.jackson.databind.ObjectMapper;
import com.flock.urja.client.PortalClient;
import com.flock.urja.error.PortalException;
import com.flock.urja.service.MeterService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.http.HttpStatus;
class MeterServiceTest {
    private final PortalClient portal = mock(PortalClient.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final MeterService service = new MeterService(portal);
    @Test void encodesSearchQuery() throws Exception {
        when(portal.getJson("/portal/meters/search?q=a%26b&page=2")).thenReturn(mapper.readTree("{\"data\":[]}"));
        assertTrue(service.search("a&b", 2).path("data").isArray());
        verify(portal).getJson("/portal/meters/search?q=a%26b&page=2");
    }
    @Test void looksUpMeterInExport() throws Exception {
        when(portal.getJson("/portal/export?page=1")).thenReturn(mapper.readTree("{\"data\":[{\"meterId\":\"J100000\",\"hierarchy\":{\"zone\":{\"code\":\"Z-01\"}}}]}"));
        assertEquals("Z-01", service.network("J100000").path("zone").path("code").asText());
    }
    @Test void missingMeterReturnsNotFound() throws Exception {
        when(portal.getJson("/portal/export?page=1")).thenReturn(mapper.readTree("{\"data\":[]}"));
        PortalException ex = assertThrows(PortalException.class, () -> service.details("MISSING"));
        assertEquals(HttpStatus.NOT_FOUND, ex.status());
    }
}
