package edu.ap.gosmartlib.services.schoolIntegration;

import edu.ap.gosmartlib.entities.SchoolIntegrationEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class SmartschoolSoapClient {

    private final RestClient restClient = RestClient.create();

    // in SmartschoolSoapClient.java

    public void sendMessage(SchoolIntegrationEntity integration, String username, String title, String body) {
        String accesscode = escapeXml(integration.getSmartschoolAccesscode());
        String sender = escapeXml(integration.getSmartschoolSenderIdentifier() != null
                ? integration.getSmartschoolSenderIdentifier() : "");
        String endpoint = integration.getOnerosterBaseUrl() + "/Webservices/V3";
        String safeUsername = escapeXml(username);
        String safeTitle = escapeXml(title);
        String safeBody = escapeXml(body);

        String soapEnvelope = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                              xmlns:ss="http://www.smartschool.be/Webservices">
              <soapenv:Body>
                <ss:sendMsg>
                  <accesscode>%s</accesscode>
                  <userIdentifier>%s</userIdentifier>
                  <title>%s</title>
                  <body>%s</body>
                  <senderIdentifier>%s</senderIdentifier>
                </ss:sendMsg>
              </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(accesscode, safeUsername, safeTitle, safeBody, sender);
        // ... rest unchanged
    }

    private static String escapeXml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

}
