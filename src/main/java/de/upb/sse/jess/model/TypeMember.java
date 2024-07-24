package de.upb.sse.jess.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TypeMember {
    private final String type;
    private final String member;

    public TypeMember(String qualifiedName) {
        // TODO: split qualified name
        this.type = "";
        this.member = "";
    }

    public boolean typeEquals(String type) {
        return this.type.equals(type);
    }

    public boolean memberEquals(String member) {
        return this.member.equals(member);
    }

    public boolean equals(TypeMember tm) {
        return this.typeEquals(tm.getType()) && this.memberEquals(tm.getMember());
    }

}
