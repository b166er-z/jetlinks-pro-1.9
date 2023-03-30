package org.jetlinks.pro.notify.email.embedded;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.notify.template.Template;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class EmailTemplate implements Template {

    private String subject;

    private String text;

    private List<Attachment> attachments;

    private List<String> sendTo;

    @Getter
    @Setter
    public static class Attachment {

        private String name;

        private String location;

    }
}
