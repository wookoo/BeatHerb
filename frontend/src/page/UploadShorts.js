import { Link } from "react-router-dom";
import { useState, useRef, useEffect } from "react";
import { uploadMusic } from "../api/upload.js";
// 음원 업로드 모달
// 향후 DaisyUI 이용해서 모달로 구현 필요
// 제목, 앨범 표지, 가사, 해시태그, 상세 정보
// 향후 해시태그 및 파일 첨부, 버튼 누르면 다른 페이지로 이동 구현 필요

export default function UploadMusic({ shorts }) {
  const [image, setImage] = useState(null);
  const [creatorIdList, setCreatorIdList] = useState([]);
  const titleRef = useRef(null);
  const videoRef = useRef(null);
  useEffect(() => {
    if (videoRef.current && shorts) {
      const videoUrl = URL.createObjectURL(shorts);
      videoRef.current.src = videoUrl;
    }
  }, [shorts]);

  const onSubmit = async () => {
    const formData = new FormData();
    formData.append("title", titleRef.current.value);
    formData.append("creatorIdList", creatorIdList);
    formData.append("image", image);
    formData.append("shorts", shorts, titleRef.current.value + ".mp4");
    await uploadMusic(formData).then();
  };
  const clear = async () => {
    setImage(null);
    setCreatorIdList([]);
    titleRef.current.value = "";
    videoRef.current.pause();
  };
  const fileImgInputRef = useRef(null);
  const onChangeImg = (event) => {
    setImage(event.target.files[0]);
  };

  return (
    <div className="w-full h-full flex">
      <div className="flex flex-col w-3/6 items-center justify-center">
        <div className="text-base-content pt-5 pb-50 w-full">
          <div className="text-4xl pb-10">쇼츠 등록하기</div>
          <div className="flex items-center pb-10 mx-6">
            <div className="text-left whitespace-nowrap pr-10">제목</div>
            <input
              type="text"
              id="title"
              ref={titleRef}
              placeholder="제목을 입력해주세요."
              className="input input-ghost w-full max-w-xs px-3"
            />
          </div>

          <div className="flex pb-10 justify-between mx-6">
            <div className="text-left whitespace-nowrap pr-10">창작가</div>
            <div className="btn btn-primary btn-xs">+ 추가하기</div>
          </div>

          <div className="flex pb-10 justify-between mx-6">
            <div className="text-left whitespace-nowrap pr-10">해시태그</div>
            <div className="btn btn-primary btn-xs">+ 추가하기</div>
          </div>

          <div className="flex pb-10 justify-between mx-6">
            <input
              type="file"
              style={{ display: "none" }}
              accept="image/jpg,impge/png,image/jpeg"
              name="cover_img"
              onChange={onChangeImg}
              ref={fileImgInputRef}
            />
            <div className="text-left whitespace-nowrap pr-10">썸네일</div>
            <button
              className="btn btn-primary btn-xs"
              onClick={() => {
                fileImgInputRef.current.click();
              }}>
              첨부하기
            </button>
          </div>

          <div className="flex justify-center">
            <div className="self-auto text-xl flex">
              <div className="modal-action px-3">
                <form method="dialog">
                  <button className="btn" onClick={onSubmit}>
                    작성하기
                  </button>
                </form>
              </div>
              <div className="modal-action px-3">
                <form method="dialog">
                  <button className="btn" onClick={clear}>
                    취소하기
                  </button>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div className="flex flex-col w-3/6 items-center justify-center">
        <video ref={videoRef} controls></video>
      </div>
    </div>
  );
}
