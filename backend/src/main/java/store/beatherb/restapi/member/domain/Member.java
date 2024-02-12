package store.beatherb.restapi.member.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import store.beatherb.restapi.content.domain.Creator;
import store.beatherb.restapi.interest.domain.Interest;
import store.beatherb.restapi.live.domain.Guest;
import store.beatherb.restapi.live.domain.Live;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j

@Entity
@Table(name="member")
@Getter
@Setter
// conflict나면 이부분은 그냥 내가 builder패턴으로 만들려고 한거니까 원래 만든거로 써도됨 -> 근데 필드에 naver, kakao, google은 추가해주세요
@NoArgsConstructor(access = AccessLevel.PROTECTED) //PROTECTED 이거 밖에서하면 access가 안돼요...
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "name")
    private String name;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "advertise")
    private Boolean advertise;


    @CreationTimestamp
    private LocalDateTime created_at;

    @UpdateTimestamp
    private LocalDateTime updated_at;

    @Setter
    @Column(name="dm_agree")
    private boolean dmAgree;

    @Setter
    @Column(name="img")
    private String image;

    @Column(name="naver")
    private String naver;

    @Column(name="kakao")
    private String kakao;

    @Column(name="google")
    private String google;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL,orphanRemoval = true)
    private Verify verify;

    @OneToMany(mappedBy = "member",fetch = FetchType.LAZY,orphanRemoval = true)
    private List<Interest> interestList;

    @OneToMany(mappedBy = "creator", fetch = FetchType.LAZY)
    private List<Creator> creatorList;
    @OneToMany(mappedBy = "member" , fetch = FetchType.LAZY,orphanRemoval = true)
    private List<Guest> liveGuestList;
    @OneToOne(mappedBy = "member" ,fetch = FetchType.LAZY,orphanRemoval = true)
    private Live live;

    public Optional<Verify> getVerify() {
        return Optional.ofNullable(verify);
    }

    @Builder
    public Member(String email, String name, String nickname, Boolean advertise, boolean dmAgree, String naver, String kakao, String google, Verify verify) {
        this.email = email;
        this.name = name;
        this.nickname = nickname;
        this.advertise = advertise;
        this.dmAgree = dmAgree;
        this.naver = naver;
        this.kakao = kakao;
        this.google = google;
        this.image = "noimage.jpeg";
        this.verify = verify;
    }
}

