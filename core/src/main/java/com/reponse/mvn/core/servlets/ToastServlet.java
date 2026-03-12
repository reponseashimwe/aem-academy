package com.reponse.mvn.core.servlets;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;

@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/bin/codenova/toast",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET
        }
)
@ServiceDescription("Toast component servlet")
public class ToastServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    protected void doGet(final SlingHttpServletRequest request,
                         final SlingHttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        String time = LocalTime.now().format(TIME_FORMATTER);
        response.getWriter().write(
                "<div class=\"cn-toast-message\">\n" +
                        "  <span class=\"cn-toast-time\">" + time + "</span>\n" +
                        "</div>"
        );
    }
}

