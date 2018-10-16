package uk.gov.hmcts.reform.bulkscan.orchestrator.services.ccd.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.bulkscan.orchestrator.services.ccd.CcdAuthenticator;
import uk.gov.hmcts.reform.bulkscan.orchestrator.services.ccd.CcdAuthenticatorFactory;
import uk.gov.hmcts.reform.bulkscan.orchestrator.services.servicebus.model.Envelope;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;

/**
 * Implementation of strategy.
 * Any strategy is invoking steps as follows:
 * <ul>
 *     <li>jurisdiction authentication</li>
 *     <li>start the event</li>
 *     <li>create data content accordingly</li>
 *     <li>submit the event</li>
 * </ul>
 * Any publisher will have to be implemented as an example:
 * <pre>{@code
 * @Component("publisher-name")
 * class PublisherName extends AbstractEventPublisher {
 *
 *     PublisherName() {
 *         // any extra autowiring needed
 *     }
 *
 *     @Override
 *     Object mapEnvelopeToCaseDataObject(Envelope envelope) {
 *         return null; // implement this
 *     }
 *
 *     @Override
 *     String getEventTypeId() {
 *         return ""
 *     }
 *
 *     @Override
 *     String getEventSummary() {
 *         return ""
 *     }
 * }}</pre>
 * <p/>
 * No need to make any public access as everything will be injected in {@link EventPublisherContainer}:
 * <pre>{@code
 * @Resource(name = "publisher-name")
 * private EventPublisher someNamedPublisher;}</pre>
 * <p/>
 * Then include each publisher in {@link EventPublisherContainer}
 */
abstract class AbstractEventPublisher implements EventPublisher {

    static final String CASE_TYPE_ID = "Bulk_Scanned";

    private static final Logger log = LoggerFactory.getLogger(AbstractEventPublisher.class);

    @Autowired
    private CoreCaseDataApi ccdApi;

    @Autowired
    private CcdAuthenticatorFactory authenticatorFactory;

    @Override
    public void publish(Envelope envelope) {
        String caseRef = envelope.caseRef;
        String jurisdiction = envelope.jurisdiction;

        logCaseCreationEntry(caseRef);

        CcdAuthenticator authenticator = authenticateJurisdiction(jurisdiction);
        StartEventResponse eventResponse = startEvent(authenticator, jurisdiction, caseRef);
        CaseDataContent caseDataContent = buildCaseDataContent(
            eventResponse.getToken(),
            mapEnvelopeToCaseDataObject(envelope)
        );
        submitEvent(authenticator, jurisdiction, caseRef, caseDataContent);
    }

    // region - execution steps

    private void logCaseCreationEntry(String caseRef) {
        log.info("Creating {} for case {}", getClass().getSimpleName(), caseRef);
    }

    private CcdAuthenticator authenticateJurisdiction(String jurisdiction) {
        return authenticatorFactory.createForJurisdiction(jurisdiction);
    }

    private StartEventResponse startEvent(
        CcdAuthenticator authenticator,
        String jurisdiction,
        String caseRef
    ) {
        return ccdApi.startEventForCaseWorker(
            authenticator.getUserToken(),
            authenticator.getServiceToken(),
            authenticator.getUserDetails().getId(),
            jurisdiction,
            CASE_TYPE_ID,
            caseRef,
            getEventTypeId()
        );
    }

    // todo perhaps some generic interface for these data objects?
    abstract Object mapEnvelopeToCaseDataObject(Envelope envelope);

    private CaseDataContent buildCaseDataContent(
        String eventToken,
        Object caseDataObject
    ) {
        return CaseDataContent.builder()
            .eventToken(eventToken)
            .event(Event.builder()
                .id(getEventTypeId())
                .summary(getEventSummary())
                .build())
            .data(caseDataObject)
            .build();
    }

    private void submitEvent(
        CcdAuthenticator authenticator,
        String jurisdiction,
        String caseRef,
        CaseDataContent caseDataContent
    ) {
        ccdApi.submitEventForCaseWorker(
            authenticator.getUserToken(),
            authenticator.getServiceToken(),
            authenticator.getUserDetails().getId(),
            jurisdiction,
            CASE_TYPE_ID,
            caseRef,
            true,
            caseDataContent
        );
    }

    // end region - execution steps

    /**
     * EventPublisher is coupled with event type id. Used in building case data.
     * @return Event type ID
     */
    abstract String getEventTypeId();

    /**
     * Short sentence representing event type ID. Used in building case data.
     * @return Event summary
     */
    abstract String getEventSummary();
}