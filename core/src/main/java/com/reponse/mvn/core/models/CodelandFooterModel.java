package com.reponse.mvn.core.models;

import java.util.Arrays;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class CodelandFooterModel {

    public String getCopyrightText() {
        return "\u00a9 Lorem ipsum dolor sit";
    }

    public List<FooterSection> getSections() {
        return Arrays.asList(
            new FooterSection("LINK", Arrays.asList(
                "Docenti",
                "Sostieni l\u2019Universit\u00e0",
                "Vita e pensiero editrice",
                "Sistema bibliotecario",
                "Librerie e merchandising",
                "Giornalisti e media",
                "Ufficio rapporti con il pubblico",
                "Contatti"
            )),
            new FooterSection("STRUMENTI", Arrays.asList(
                "Off-campus",
                "CV Online",
                "Albo Fornitori",
                "Bandi e Gare",
                "Verifica certificati e autocertificazioni",
                "Intranet"
            )),
            new FooterSection("SOCIAL E MEDIA @UNICATT", Arrays.asList(
                "Facebook",
                "X",
                "Linkedin",
                "Youtube",
                "Instagram",
                "Telegram",
                "Spotify",
                "Presenza"
            )),
            new FooterSection("SECONDO TEMPO", Arrays.asList(
                "Media center",
                "Presenza",
                "Facebook",
                "X",
                "Instagram"
            ))
        );
    }

    public List<LegalLink> getLegalLinks() {
        return Arrays.asList(
            new LegalLink("Privacy", null),
            new LegalLink("Cookies", null),
            new LegalLink("Accessibilit\u00e0", null),
            new LegalLink("Impostazione dei Cookies", null)
        );
    }

    public static final class FooterSection {
        private final String title;
        private final List<String> items;

        public FooterSection(String title, List<String> items) {
            this.title = title;
            this.items = items;
        }

        public String getTitle() { return title; }
        public List<String> getItems() { return items; }
    }

    public static final class LegalLink {
        private final String label;
        private final String url;

        public LegalLink(String label, String url) {
            this.label = label;
            this.url = url;
        }

        public String getLabel() { return label; }
        public String getUrl() { return url; }
    }
}
