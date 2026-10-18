package org.example.lab3.repository;

import org.example.lab3.domain.Member;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Simple in-memory member store. */
@Repository
public class MemberRepository {

    private final Map<String, Member> store = new HashMap<>();

    public void save(Member member)                    { store.put(member.getId(), member); }
    public Optional<Member> findById(String id)        { return Optional.ofNullable(store.get(id)); }
    public boolean existsById(String id)               { return store.containsKey(id); }
}
