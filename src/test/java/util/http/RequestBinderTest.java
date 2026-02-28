package util.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;

class RequestBinderTest {

    @Test
    void bind_shouldMapPassengersAndMaitreIndex() throws Exception {
        HttpServletRequest request = mockRequest(Map.of(
            "passengers[0].nom", new String[] { "Rakoto" },
            "passengers[0].prenoms", new String[] { "Jean" },
            "passengers[1].nom", new String[] { "Rabe" },
            "passengers[1].prenoms", new String[] { "Marie" },
            "maitreIndex", new String[] { "1" }
        ));

        ReservationPayload payload = RequestBinder.bind(request, ReservationPayload.class);

        assertNotNull(payload.getPassengers());
        assertEquals(2, payload.getPassengers().size());
        assertEquals("Rakoto", payload.getPassengers().get(0).getNom());
        assertEquals("Marie", payload.getPassengers().get(1).getPrenoms());
        assertEquals(1, payload.getMaitreIndex());
    }

    @Test
    void bind_shouldSupportTwoLevelsOfIndex() throws Exception {
        HttpServletRequest request = mockRequest(Map.of(
            "matrix[0][1]", new String[] { "A" },
            "matrix[1][0]", new String[] { "B" }
        ));

        MatrixPayload payload = RequestBinder.bind(request, MatrixPayload.class);

        assertNotNull(payload.getMatrix());
        assertEquals("A", payload.getMatrix().get(0).get(1));
        assertEquals("B", payload.getMatrix().get(1).get(0));
    }

    @Test
    void bind_shouldSupportNLevelsOfIndex() throws Exception {
        Map<String, String[]> params = new LinkedHashMap<>();
        params.put("cube[1][0][2]", new String[] { "X" });
        params.put("cube[0][1][1]", new String[] { "Y" });

        HttpServletRequest request = mockRequest(params);

        CubePayload payload = RequestBinder.bind(request, CubePayload.class);

        assertNotNull(payload.getCube());
        assertEquals("X", payload.getCube().get(1).get(0).get(2));
        assertEquals("Y", payload.getCube().get(0).get(1).get(1));
    }

    private HttpServletRequest mockRequest(Map<String, String[]> params) {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getParameterMap()).thenReturn(params);
        return req;
    }

    public static class ReservationPayload {
        private List<PassengerPayload> passengers;
        private Integer maitreIndex;

        public List<PassengerPayload> getPassengers() {
            return passengers;
        }

        public void setPassengers(List<PassengerPayload> passengers) {
            this.passengers = passengers;
        }

        public Integer getMaitreIndex() {
            return maitreIndex;
        }

        public void setMaitreIndex(Integer maitreIndex) {
            this.maitreIndex = maitreIndex;
        }
    }

    public static class PassengerPayload {
        private String nom;
        private String prenoms;

        public String getNom() {
            return nom;
        }

        public void setNom(String nom) {
            this.nom = nom;
        }

        public String getPrenoms() {
            return prenoms;
        }

        public void setPrenoms(String prenoms) {
            this.prenoms = prenoms;
        }
    }

    public static class MatrixPayload {
        private List<List<String>> matrix;

        public List<List<String>> getMatrix() {
            return matrix;
        }

        public void setMatrix(List<List<String>> matrix) {
            this.matrix = matrix;
        }
    }

    public static class CubePayload {
        private List<List<List<String>>> cube;

        public List<List<List<String>>> getCube() {
            return cube;
        }

        public void setCube(List<List<List<String>>> cube) {
            this.cube = cube;
        }
    }
}