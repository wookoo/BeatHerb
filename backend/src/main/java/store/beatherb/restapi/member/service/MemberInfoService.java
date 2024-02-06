package store.beatherb.restapi.member.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;
import store.beatherb.restapi.global.exception.BeatHerbErrorCode;
import store.beatherb.restapi.global.exception.BeatHerbException;
import store.beatherb.restapi.member.domain.Member;
import store.beatherb.restapi.member.domain.MemberRepository;
import store.beatherb.restapi.member.dto.MemberDTO;
import store.beatherb.restapi.member.dto.request.EditRequest;
import store.beatherb.restapi.member.exception.MemberErrorCode;
import store.beatherb.restapi.member.exception.MemberException;
import store.beatherb.restapi.oauth.dto.Provider;
import store.beatherb.restapi.oauth.dto.request.OAuthRequest;
import store.beatherb.restapi.oauth.service.OAuthService;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberInfoService {

    private final MemberRepository memberRepository;
    private final OAuthService oauthService;
    @Value("${resource.directory}")
    private String REFERENCE_DIRECTORY;

    //회원 정보 수정
    @Transactional
    public void edit(MemberDTO memberDTO, EditRequest editRequest) {

        String nickname = editRequest.getNickname() != null? editRequest.getNickname() : memberDTO.getNickname();
        Boolean isDmAgree = editRequest.getDmAgree() != null? editRequest.getDmAgree() : memberDTO.getDmAgree();

        Member member = memberRepository.findById(memberDTO.getId())
                .orElseThrow(()->new MemberException(MemberErrorCode.MEMBER_FIND_ERROR));

        if(nickname==null){
            throw new MemberException(MemberErrorCode.NICKNAME_IS_NOT_EXIST);
        }

        member.setNickname(nickname);
        member.setDmAgree(isDmAgree);

        memberRepository.save(member);

        // 요청 시 이미지가 있는 경우에만, 이미지 저장
        if(editRequest.getPicture() != null){

            MultipartFile picture = editRequest.getPicture();
            String fileName = picture.getOriginalFilename();
            String format = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();

            String saveFileName = member.getId() + format;

            File file = new File(REFERENCE_DIRECTORY + File.separator + saveFileName);

            try {
                FileCopyUtils.copy(picture.getBytes(), file);
            } catch (IOException e) {
                throw new BeatHerbException(BeatHerbErrorCode.INTERNAL_SERVER_ERROR);
            }

        }


    }

    //회원 정보 수정 - 소셜 로그인 연동
    public void linkage(OAuthRequest oauthRequest, Provider provider, MemberDTO memberDTO) {
        Long id;//헤더에서 access token을 받아와서 id값 가져오기. 토큰값 확인
        log.info("사용자 정보"+memberDTO.getId());
        Member member=memberRepository.findById(memberDTO.getId())
                .orElseThrow(()->new MemberException(MemberErrorCode.MEMBER_FIND_ERROR));
        String sub=oauthService.sub(oauthRequest,provider);
        switch(provider){
            case KAKAO -> {
                memberRepository.findByKakao(sub)
                        .ifPresent(i->{throw new MemberException(MemberErrorCode.SOCIAL_EXIST);});
                member.setKakao(sub);
            }
            case NAVER -> {
                memberRepository.findByNaver(sub)
                        .ifPresent(i->{throw new MemberException(MemberErrorCode.SOCIAL_EXIST);});
                member.setNaver(sub);
            }
            case GOOGLE -> {
                memberRepository.findByGoogle(sub)
                        .ifPresent(i->{throw new MemberException(MemberErrorCode.SOCIAL_EXIST);});
                member.setGoogle(sub);
            }
        }
        memberRepository.save(member);
    }

}
