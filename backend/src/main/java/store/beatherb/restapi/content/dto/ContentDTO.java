package store.beatherb.restapi.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import store.beatherb.restapi.content.domain.*;
import store.beatherb.restapi.member.dto.MemberDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class ContentDTO {
    private Long id;
    private List<CreatorDTO> creators;
    private String title;
    private MemberDTO writer;
    private ContentTypeDTO contentType;
    private List<ContentDTO> inorder;
    private List<HashTagDTO> hashTags;
    private String lyrics;
    private String describe;
    private int hit;
    private LocalDateTime createdAt;

    public static ContentDTO toDto(Content entity){

        List<ContentDTO> contentDTOs = new ArrayList<>();
        for (InOrder c : entity.getInOrderList()){
            ContentDTO dto = ContentDTO.toDto(c.getChildContent());
            contentDTOs.add(dto);
        }

        List<CreatorDTO> creatorDTOs = new ArrayList<>();
        for (Creator c : entity.getCreatorList()){
            CreatorDTO dto = CreatorDTO.toDto(c);
            creatorDTOs.add(dto);
        }

        List<HashTagDTO> hashTagDTOS = new ArrayList<>();
        for (ContentHashTag c : entity.getContentHashTagList()){
            HashTagDTO dto = HashTagDTO.toDto(c.getHashTag());
            hashTagDTOS.add(dto);
        }

        return ContentDTO.builder()
              .id(entity.getId())
              .creators(creatorDTOs)
              .title(entity.getTitle())
              .writer(MemberDTO.toDTO(entity.getWriter()))
              .contentType(ContentTypeDTO.toDto(entity.getContentType()))
              .inorder(contentDTOs)
              .hashTags(hashTagDTOS)
              .lyrics(entity.getLyrics())
              .describe(entity.getDescribe())
              .hit(entity.getHit())
              .createdAt(entity.getCreatedAt())
              .build();
    }
}
