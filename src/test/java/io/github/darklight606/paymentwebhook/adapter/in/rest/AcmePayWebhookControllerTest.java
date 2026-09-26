package io.github.darklight606.paymentwebhook.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.darklight606.paymentwebhook.domain.exception.InvalidPayloadException;
import io.github.darklight606.paymentwebhook.domain.exception.InvalidSignatureException;
import io.github.darklight606.paymentwebhook.domain.port.in.ReceiveWebhookCommand;
import io.github.darklight606.paymentwebhook.domain.port.in.ReceiveWebhookResult;
import io.github.darklight606.paymentwebhook.domain.port.in.ReceiveWebhookUseCase;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AcmePayWebhookController.class)
class AcmePayWebhookControllerTest {

    private static final String SIGNATURE_HEADER_NAME = "AcmePay-Signature";
    private static final byte[] RAW_BODY = "{\"id\":\"evt_1\"}".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReceiveWebhookUseCase receiveWebhookUseCase;

    @Test
    void receive_newEvent_returns200() throws Exception {
        when(receiveWebhookUseCase.receive(any())).thenReturn(new ReceiveWebhookResult("evt_1", false));

        mockMvc.perform(post("/webhooks/acmepay")
                        .header(SIGNATURE_HEADER_NAME, "t=1,v1=abc")
                        .content(RAW_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void receive_duplicateEvent_returns200() throws Exception {
        when(receiveWebhookUseCase.receive(any())).thenReturn(new ReceiveWebhookResult("evt_1", true));

        mockMvc.perform(post("/webhooks/acmepay")
                        .header(SIGNATURE_HEADER_NAME, "t=1,v1=abc")
                        .content(RAW_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void receive_invalidSignature_returns401ProblemJson() throws Exception {
        when(receiveWebhookUseCase.receive(any())).thenThrow(new InvalidSignatureException("signature mismatch"));

        mockMvc.perform(post("/webhooks/acmepay")
                        .header(SIGNATURE_HEADER_NAME, "t=1,v1=abc")
                        .content(RAW_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("webhook signature verification failed"));
    }

    @Test
    void receive_invalidPayload_returns400ProblemJson() throws Exception {
        when(receiveWebhookUseCase.receive(any())).thenThrow(new InvalidPayloadException("payload invalid"));

        mockMvc.perform(post("/webhooks/acmepay")
                        .header(SIGNATURE_HEADER_NAME, "t=1,v1=abc")
                        .content(RAW_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("payload invalid"));
    }

    @Test
    void receive_rawBytes_reachUseCaseUnchanged() throws Exception {
        when(receiveWebhookUseCase.receive(any())).thenReturn(new ReceiveWebhookResult("evt_1", false));

        mockMvc.perform(post("/webhooks/acmepay")
                        .header(SIGNATURE_HEADER_NAME, "t=1,v1=abc")
                        .content(RAW_BODY))
                .andExpect(status().isOk());

        ArgumentCaptor<ReceiveWebhookCommand> captor = ArgumentCaptor.forClass(ReceiveWebhookCommand.class);
        verify(receiveWebhookUseCase).receive(captor.capture());
        assertThat(captor.getValue().signatureHeader()).isEqualTo("t=1,v1=abc");
        assertThat(captor.getValue().rawBody()).isEqualTo(RAW_BODY);
    }
}
