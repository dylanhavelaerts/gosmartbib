package edu.ap.gosmartlib.services.schoolintegration;

import edu.ap.gosmartlib.entities.school.SchoolIntegrationEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Lage-niveau client voor de Smartschool SOAP V3 Web Service (sendMsg-operatie).
 * Bouwt het SOAP-envelope manueel op en ontsnapt alle invoer om XML-injectie te voorkomen.
 * Fouten worden gelogd maar niet doorgegooid zodat een mislukt bericht het normale verloop niet onderbreekt.
 */
@Service
@Slf4j
public class SmartschoolSoapClient {

    private final RestClient restClient = RestClient.create();


    public void sendMessage(SchoolIntegrationEntity integration, String senderIdentifier,
                            String username, String title, String body) {
        String accesscode = escapeXml(integration.getSmartschoolAccesscode());
        String senderPart = (senderIdentifier != null && !senderIdentifier.isBlank())
                ? "<senderIdentifier>%s</senderIdentifier>".formatted(escapeXml(senderIdentifier))
                : "";
        String endpoint = integration.getSchoolBaseUrl() + "/Webservices/V3";
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
                      %s
                    </ss:sendMsg>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(accesscode, safeUsername, safeTitle, safeBody, senderPart);

        try {
            String response = restClient.post()
                    .uri(endpoint)
                    .header("Content-Type", "text/xml;charset=UTF-8")
                    .header("SOAPAction", "\"\"")
                    .body(soapEnvelope)
                    .retrieve()
                    .body(String.class);
            log.info("SOAP bericht verstuurd: {}", response);
        } catch (Exception e) {
            log.error("SOAP bericht versturen mislukt: {}", e.getMessage());
        }

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