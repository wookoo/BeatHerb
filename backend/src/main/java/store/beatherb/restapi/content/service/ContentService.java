package store.beatherb.restapi.content.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;
import store.beatherb.restapi.content.domain.*;
import store.beatherb.restapi.content.domain.embed.ContentTypeEnum;
import store.beatherb.restapi.content.dto.request.CreatorAgreeRequest;
import store.beatherb.restapi.content.dto.request.ContentUploadRequest;
import store.beatherb.restapi.content.dto.respone.ContentUploadRespone;
import store.beatherb.restapi.content.dto.response.ContentResponse;
import store.beatherb.restapi.content.dto.response.ContentListInterface;
import store.beatherb.restapi.content.exception.*;
import store.beatherb.restapi.global.auth.exception.AuthErrorCode;
import store.beatherb.restapi.global.auth.exception.AuthException;
import store.beatherb.restapi.global.exception.BeatHerbErrorCode;
import store.beatherb.restapi.global.exception.BeatHerbException;
import store.beatherb.restapi.global.validate.MusicValid;
import store.beatherb.restapi.infrastructure.kafka.domain.dto.response.ContentUploadToKafkaResponse;
import store.beatherb.restapi.infrastructure.kafka.service.KafkaProducerService;
import store.beatherb.restapi.member.domain.Member;
import store.beatherb.restapi.member.domain.MemberRepository;
import store.beatherb.restapi.member.dto.MemberDTO;
import store.beatherb.restapi.member.exception.MemberErrorCode;
import store.beatherb.restapi.member.exception.MemberException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@Slf4j
public class ContentService {


    private final ContentRepository contentRepository;
    private final MemberRepository memberRepository;

    private final HashTagRepository hashTagRepository;

    private final CreatorRepository creatorRepository;

    private final KafkaProducerService kafkaProducerService;

    private final PlayRepository playRepository;

    private final ContentTypeRepository contentTypeRepository;

    private final String CROPPED_DIRECTORY;
    private final String REFERENCE_DIRECTORY;


//
//    private final String FFMPEG_LOCATION;
//    private final String FFPROBE_LOCATION;


    public ContentService(ContentRepository contentRepository,
                          MemberRepository memberRepository,
                          HashTagRepository hashTagRepository,
                          KafkaProducerService kafkaProducerService,
                          CreatorRepository creatorRepository,
                          PlayRepository playRepository,
                          ContentTypeRepository contentTypeRepository,
                          @Value("${resource.directory.music.cropped}") String CROPPED_DIRECTORY,
                          @Value("${resource.directory.music.reference}") String REFERENCE_DIRECTORY
//                          @Value("${ffmpeg.directory.ffmpeg}") String FFMPEG_LOCATION,
//                          @Value("${ffmpeg.directory.ffprobe}") String FFPROBE_LOCATION
    ) {
        this.contentRepository = contentRepository;
        this.memberRepository = memberRepository;
        this.hashTagRepository = hashTagRepository;
        this.creatorRepository = creatorRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.playRepository = playRepository;
        this.contentTypeRepository = contentTypeRepository;
        this.CROPPED_DIRECTORY = CROPPED_DIRECTORY;
        this.REFERENCE_DIRECTORY = REFERENCE_DIRECTORY;
//        this.FFMPEG_LOCATION = FFMPEG_LOCATION;
//        this.FFPROBE_LOCATION = FFPROBE_LOCATION;
    }


    public List<Content> getContentsOrderByHit() {
        Optional<List<Content>> contentListOptional = contentRepository.findAllByOrderByHitDesc();
        return contentListOptional.orElseThrow(() -> new ContentException(ContentErrorCode.LIST_HAS_NOT_CONTENT));
    }

    public Resource getPlayList(Integer contentNumber) {
        String fileFullPath = CROPPED_DIRECTORY +
                "/" +
                contentNumber +
                "/playlist.m3u8";

        Resource resource = new FileSystemResource(fileFullPath);
        if (resource.exists()) {
            return resource;
        }
        throw new ContentException(ContentErrorCode.CONTENT_NOT_FOUND);
    }

    public Resource getSegmentByContentNumberAndSegmentName(Integer contentNumber, String segmentName) {
        String fileFullPath = CROPPED_DIRECTORY + "/" + contentNumber + "/" + segmentName;
        Resource resource = new FileSystemResource(fileFullPath);
        if (resource.exists()) {
            return resource;
        }
        throw new ContentException(ContentErrorCode.CONTENT_NOT_FOUND);
    }


    @Transactional
    public ContentUploadRespone uploadContent(MemberDTO memberDTO, ContentUploadRequest request) {
        if (!MusicValid.isMusicFile(request.getMusic())) {
            throw new ContentException(ContentErrorCode.MUSIC_NOT_VALID);
        }
        Member writer = memberRepository.findById(memberDTO.getId()).
                orElseThrow(
                        () -> {
                            return new MemberException(MemberErrorCode.MEMBER_FIND_ERROR);
                        }
                );
        Set<Long> creatorIds = request.getCreatorIds();
        Creator baseCreator = Creator.builder()
                .creator(writer)
                .agree(true)
                .build();
        List<Creator> creators = new ArrayList<>();
        creators.add(baseCreator);
        for (Long creatorId : creatorIds) { //창작가들을 찾을수 없으면 Throw
            Member member = memberRepository.findById(creatorId)
                    .orElseThrow(
                            () -> {
                                return new MemberException(MemberErrorCode.MEMBER_FIND_ERROR);
                            }
                    );
            creators.add(
                    Creator.builder()
                            .creator(member)
                            .agree(false)
                            .build()
            );
        }

        Set<Long> hashTagsIds = request.getHashTagIds();
        List<HashTag> hashTags = new ArrayList<>();


        for (Long hashTagId : hashTagsIds) {
            HashTag hashTag = hashTagRepository.findById(hashTagId)
                    .orElseThrow(
                            () -> {
                                return new ContentException(ContentErrorCode.HASHTAG_NOT_FOUND);
                            }
                    );
            hashTags.add(hashTag);
        }


        Content content = Content.builder()
                .hit(0)
                .describe(request.getDescribe())
                .title(request.getTitle())
                .lyrics(request.getLyrics())
                .creators(creators)
                .writer(writer)
                .hashTags(hashTags)
                .build();

        MultipartFile music = request.getMusic();

        String fileName = music.getOriginalFilename();

        String format = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        log.info("Music File 확장자 : [{}]", format);

        contentRepository.save(content);

        String saveFileName = content.getId() + format;
        File file = new File(REFERENCE_DIRECTORY + File.separator + saveFileName);
        try {
            FileCopyUtils.copy(music.getBytes(), file);
        } catch (IOException e) {
            throw new BeatHerbException(BeatHerbErrorCode.INTERNAL_SERVER_ERROR);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ContentUploadToKafkaResponse contentUploadToKafkaResponse = ContentUploadToKafkaResponse.builder()
                    .writerId(writer.getId())
                    .fileName(saveFileName)
                    .build();

            String contentUploadToKafkaResponseJson = objectMapper.writeValueAsString(contentUploadToKafkaResponse);
            kafkaProducerService.sendProcessingHls(contentUploadToKafkaResponseJson);


            //동의 비동의 처리

        } catch (JsonProcessingException e) {
            throw new BeatHerbException(BeatHerbErrorCode.INTERNAL_SERVER_ERROR);
        }

        return ContentUploadRespone.builder().id(content.getId()).build();

    }


    @Transactional
    public void contentAgreeAboutCreator(MemberDTO memberDTO, CreatorAgreeRequest creatorAgreeRequest) {

        Member member = memberRepository.findById(memberDTO.getId()).orElseThrow(
                () -> {
                    return new MemberException(MemberErrorCode.MEMBER_FIND_ERROR);
                }
        );

        Creator creator = creatorRepository.findById(creatorAgreeRequest.getId()).orElseThrow(
                () -> {
                    return new CreatorException(CreatorErrorCode.CREATOR_NOT_FOUND);
                }
        );

        if (creator.getCreator() != member) {
            throw new AuthException(AuthErrorCode.PERMISSION_DENIED);
        }
        if (creator.isAgree()) {
            throw new CreatorException(CreatorErrorCode.ALREADY_AGREE);
        }
        if (!creatorAgreeRequest.getAgree()) {
            creatorRepository.delete(creator);
        } else {
            creator.setAgree(true);
            creatorRepository.save(creator);
        }

    }


//    public void makeSequence(){ //너무 느려서 다른 녀석이 처리해줘야함 너무느림.
//
//        try {
//            FFprobe ffprobe = new FFprobe(FFPROBE_LOCATION);
//            FFmpeg ffmpeg = new FFmpeg(FFMPEG_LOCATION);
//            FFmpegBuilder builder = new FFmpegBuilder()
//                    .setInput(REFERENCE_DIRECTORY+"/1.mp3")
//                    .overrideOutputFiles(true)
//                    .addOutput(CROPPED_DIRECTORY + "/1/playlist.m3u8")
//                    .setAudioCodec("aac")
//                    .setAudioBitRate(192000)
//                    .setFormat("hls")
//                    .addExtraArgs("-hls_list_size", "0")
//                    .addExtraArgs("-hls_base_url", "1/")
//                    .addExtraArgs("-hls_time", "10")
//                    .addExtraArgs("-hls_segment_filename", CROPPED_DIRECTORY +"/1/%03d.aac")
//                    .done();
//
//            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
//            executor.createJob(builder).run();
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
//
//    }

    //daily 인기 차트 가져오기
    public List<ContentResponse> getPopularityDaily(ContentTypeEnum contentTypeEnum){
            ContentType contentType = contentTypeRepository.findByType(contentTypeEnum)
                    .orElseThrow(() -> new ContentTypeException(ContentTypeErrorCode.CONTENT_TYPE_NOT_FOUND));

            Date nowDate = new Date();
            SimpleDateFormat todayDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String strNowDate = todayDateFormat.format(nowDate);
            List<ContentListInterface> contentList = playRepository.findByCreatedAtDate(strNowDate, contentType.getId());

            List<ContentResponse> content = new ArrayList<>();
            for(ContentListInterface response : contentList){
            Member findMember = memberRepository.findById(response.getContentWriterId())
                    .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_FIND_ERROR));

            content.add(ContentResponse.builder().title(response.getTitle()).name(findMember.getName()).build());
        }

        return content;
    }

    //weekly 인기 차트 가져오기
    public List<ContentResponse> getPopularityWeekly(ContentTypeEnum contentTypeEnum) {
        ContentType contentType = contentTypeRepository.findByType(contentTypeEnum)
                .orElseThrow(() -> new ContentTypeException(ContentTypeErrorCode.CONTENT_TYPE_NOT_FOUND));

        String[] week = whatWeekPeriod();
        List<ContentListInterface> contentList = playRepository.findByCreatedAtPeriod(contentType.getId(), week[0], week[1]);

        List<ContentResponse> content = new ArrayList<>();
        for(ContentListInterface response : contentList){
            Member findMember = memberRepository.findById(response.getContentWriterId())
                    .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_FIND_ERROR));

            content.add(ContentResponse.builder().title(response.getTitle()).name(findMember.getName()).build());
        }

        return content;
    }

    //monthly 인기 차트 가져오기
    public List<ContentResponse> getPopularityMonthly(ContentTypeEnum contentTypeEnum) {
        ContentType contentType = contentTypeRepository.findByType(contentTypeEnum)
                .orElseThrow(() -> new ContentTypeException(ContentTypeErrorCode.CONTENT_TYPE_NOT_FOUND));

        String[] month = whatMonthPeriod();
        List<ContentListInterface> contentList = playRepository.findByCreatedAtPeriod(contentType.getId(), month[0], month[1]);

        List<ContentResponse> content = new ArrayList<>();
        for(ContentListInterface response : contentList){
            Member findMember = memberRepository.findById(response.getContentWriterId())
                    .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_FIND_ERROR));

            content.add(ContentResponse.builder().title(response.getTitle()).name(findMember.getName()).build());
        }

        return content;
    }

    // 현재 날짜가 어떤 주에 속해 있는가?
    private static String[] whatWeekPeriod(){
        LocalDate currentDate = LocalDate.now();

        LocalDate startOfWeek = currentDate.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        String[] week = new String[2];
        week[0] = startOfWeek.toString();
        week[1] = endOfWeek.toString();

        return week;
    }

    // 현재 날짜가 어떤 달에 속해 있는가?
    private static String[] whatMonthPeriod(){
        LocalDate currentDate = LocalDate.now();

        LocalDate startOfMonth = currentDate.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate endOfMonth = currentDate.with(TemporalAdjusters.lastDayOfMonth());

        String[] month = new String[2];
        month[0] = startOfMonth.toString();
        month[1] = endOfMonth.toString();

        return month;
    }
}

