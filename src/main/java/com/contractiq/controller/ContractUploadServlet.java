package com.contractiq.controller;

import com.contractiq.exception.ValidationException;
import com.contractiq.model.Contract;
import com.contractiq.service.ExtractionWorkflowService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

@WebServlet(name = "ContractUploadServlet", urlPatterns = {"/api/contracts/upload"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 2, // 2MB memory threshold
        maxFileSize = 1024 * 1024 * 15,      // Must match the documented default application limit.
        maxRequestSize = 1024 * 1024 * 16    // Allows multipart overhead for one 15MB PDF.
)
public class ContractUploadServlet extends BaseServlet {
    private static final Logger logger = LoggerFactory.getLogger(ContractUploadServlet.class);

    private final ExtractionWorkflowService workflowService;

    public ContractUploadServlet() {
        this.workflowService = new ExtractionWorkflowService();
    }

    public ContractUploadServlet(ExtractionWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Part filePart = req.getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                throw new ValidationException("No contract file uploaded. Please select a PDF document.");
            }

            String originalFileName = filePart.getSubmittedFileName();
            if (originalFileName == null || originalFileName.trim().isEmpty()) {
                originalFileName = "uploaded_contract.pdf";
            }

            String clientIp = req.getRemoteAddr();
            logger.info("Received contract upload request: '{}' ({} bytes) from IP {}", originalFileName, filePart.getSize(), clientIp);

            try (InputStream is = filePart.getInputStream()) {
                Contract savedContract = workflowService.processContractUpload(originalFileName, is, clientIp);
                sendCreated(resp, "Contract uploaded and analyzed successfully. Pending human review.", savedContract);
            }

        } catch (IllegalStateException e) {
            // Tomcat raises IllegalStateException before getPart() returns if
            // the multipart request exceeds the annotated request/file limit.
            sendError(resp, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "Uploaded file exceeds the 15 MB limit.");
        } catch (Exception e) {
            handleException(resp, e);
        }
    }
}
