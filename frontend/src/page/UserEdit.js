import { useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";

// 유저 정보 수정 로직
// 프로필 사진, 닉네임, DM 수신 여부
// 각 버튼을 눌렀을 때 정보를 State에 담아줌
// 수정하기, 취소 버튼을 누르면 메인 페이지로 redirect
// 이미지 업로드 기능도 구현

export default function MyPage() {
  const navigate = useNavigate();
  // 프로필 이미지
  const defaultImg =
    "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png";
  const [image, setImage] = useState(defaultImg);

  const [file, setFile] = useState(null);
  const fileInput = useRef(null);

  const onChangeImg = (e) => {
    if (e.target.files[0]) {
      setFile(e.target.files[0]);
    } else {
      // 업로드 취소할 시
      setImage(defaultImg);
      return;
    }

    // 화면에 프로필 사진 표시
    const reader = new FileReader();
    reader.onload = () => {
      if (reader.readyState === 2) {
        setImage(reader.result);
      }
    };
    reader.readAsDataURL(e.target.files[0]);
  };

  // 닉네임
  const nicknameRef = useRef(null);

  // DM 여부
  const [isActive, setIsActive] = useState(true); // 여기서 true 또는 false에 따라 버튼 활성화 여부 결정
  const handleToggle = () => {
    setIsActive(!isActive); // 상태값을 변경하여 버튼 활성화 여부 토글
  };

  // 수정하기 버튼 누를 때
  const onSubmit = async () => {
    const formData = new FormData();

    formData.append("picture", file);

    // 현재 닉네임에 입력된 값
    const nicknameValue = nicknameRef.current.value;

    formData.append("dmAgree", isActive);
    formData.append("nickname", nicknameValue);

    //const serverURL = process.env.REACT_APP_USEREDIT_SERVER_URL;

    // for (let key of formData.keys()) {
    //   console.log(key, ":", formData.get(key));
    // }

    // 서버에서 file null 체크 필요
    // 프론트에서 file 없을 시 null로 넘어감을 확인
    await axios({
      method: "PUT",
      url: "https://node5.wookoo.shop/api/member",
      mode: "cors",
      headers: {
        Authorization: "Bearer apple",
      },
      data: formData, // data 전송시에 반드시 생성되어 있는 formData 객체만 전송 하여야 한다.
    });
  };

  return (
    <div className="absolute w-full right-1/2 bottom-1/2 translate-x-1/2 translate-y-1/2">
      <div className="flex flex-col w-full items-center justify-center">
        <div className="text-base-content pt-5 pb-50">
          <div className="text-4xl pb-10">회원 정보 수정</div>
          <div className="flex justify-center pb-5" width="200" height="200">
            <img
              src={image}
              alt="Profile"
              style={{
                margin: "20px",
                width: "200px",
                height: "200px",
                borderRadius: "50%",
              }}
              onClick={() => {
                fileInput.current.click();
              }}
              className="cursor-pointer"
            />

            <input
              type="file"
              style={{ display: "none" }}
              accept="image/jpg,impge/png,image/jpeg"
              name="profile_img"
              onChange={onChangeImg}
              ref={fileInput}
            />
          </div>
          <div className="flex items-center justify-center pb-10">
            <div className="text-left whitespace-nowrap pr-7 ml-6">닉네임</div>
            <input
              type="text"
              id="nickname"
              ref={nicknameRef}
              placeholder="닉네임을 입력해주세요."
              className="input input-ghost w-full max-w-xs px-3"
            />
          </div>
          <div className="flex items-center pb-12">
            {/* 향후 관심사 기능 추가 필요 */}
            <div className="text-left whitespace-nowrap pr-11 ml-6">관심사</div>
            <div className="btn btn-primary btn-xs">+ 추가하기</div>
          </div>
          <div className="flex items-center pb-20">
            <div className="text-left whitespace-nowrap pr-16 ml-6">DM</div>
            <div role="tablist" className="tabs tabs-boxed tabs-sm">
              <button
                role="tab"
                className={`tab ${isActive ? "tab-active" : ""} mr-1`}
                onClick={handleToggle}
              >
                Yes
              </button>
              <button
                role="tab"
                className={`tab ${!isActive ? "tab-active" : ""}`}
                onClick={handleToggle}
              >
                No
              </button>
            </div>
          </div>
          <div className="flex justify-center">
            <div className="self-auto text-xl flex">
              {/* 파일 업로드 로직 구현 필요 */}
              {/* <Link to="/"> */}
              <div onClick={onSubmit} className="px-8 hover:cursor-pointer">
                수정하기
              </div>
              <div className="px-8 hover:cursor-pointer" onClick={() => navigate(-1)}>취소하기</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
