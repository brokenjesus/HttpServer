import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestParse {
    @Test
    public void testExtractFileData() {
        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
        String body = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"test.jpg\"\r\n" +
                "Content-Type: image/jpeg\r\n\r\n" +
                "dumqiwudgiquwgdiuqwgdiuqwgdiouqwGDOIUgwefiogqaewr ioufgqriougf qioeurg ioquegriou qegrgiouqer my file content here\r\n" +
                "--" + boundary + "--";
        byte[] bodyBytes = body.getBytes();

        byte[] fileData = extractFileData(bodyBytes, boundary);

        assertNotNull(fileData);
        String fileContent = new String(fileData);
        assertTrue(fileContent.contains("dumqiwudgiquwgdiuqwgdiuqwgdiouqwGDOIUgwefiogqaewr ioufgqriougf qioeurg ioquegriou qegrgiouqer my file content here"));
    }


    private byte[] extractFileData(byte[] body, String boundary) {
        String boundaryMarker = "--" + boundary;
        int startIdx = new String(body).indexOf(boundaryMarker) + boundaryMarker.length();
        int endIdx = new String(body).indexOf("--" + boundary + "--");

        if (startIdx != -1 && endIdx != -1) {
            String filePart = new String(body, startIdx, endIdx - startIdx);
            int contentStartIdx = filePart.indexOf("\r\n\r\n") + 4;
            if (contentStartIdx != -1) {
                return filePart.substring(contentStartIdx).getBytes();
            }
        }
        return null;
    }
}
