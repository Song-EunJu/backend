package com.example.demo.mypage;
import com.example.demo.dto.ResponseMessage;
import com.example.demo.mypage.dto.*;
import com.example.demo.study.domain.StudyPost;
import com.example.demo.study.service.StudyPostService;
import com.example.demo.user.User;
import com.example.demo.user.UserDetailsServiceImpl;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = {"마이페이지 API"})
@RestController
@RequiredArgsConstructor
@Transactional
@RequestMapping("/users")
public class MyPageController {
    private final UserDetailsServiceImpl userService;
    private final StudyPostService studyPostService;
    private final MyPageService myPageService;
    private final ImageService imageService;

    @GetMapping("/{userId}") // 마이페이지 정보 수정 페이지 조회
    public ResponseEntity<ResponseMessage> getProfile(@PathVariable Long userId){
        User user = userService.findUserById(userId);
        if(user==null)
            return new ResponseEntity<>(new ResponseMessage(404,"존재하지 않는 유저"),HttpStatus.OK);
        MyInfoResponse myInfo = myPageService.getProfile(user);
        return new ResponseEntity<>(ResponseMessage.withData(200,"회원정보 조회 성공", myInfo),HttpStatus.OK);
    }

    @PatchMapping("/{userId}") // 회원정보수정
    public ResponseEntity<ResponseMessage> editProfile(
            @ModelAttribute MyInfoEditDto myInfoEditDto,
            @PathVariable Long userId) throws FileUploadException {
        User user = userService.findUserById(userId);
        if(user==null)
            return new ResponseEntity<>(new ResponseMessage(404,"존재하지 않는 유저"),HttpStatus.OK);

        String password = myInfoEditDto.getPassword();
        String newPassword = myInfoEditDto.getNewPassword();
        String confirmPassword = myInfoEditDto.getConfirmPassword();
        if(newPassword!=null && confirmPassword!=null){ // 이게 들어온 경우

        }

//        myPageService.checkPassword(user, password, newPassword, confirmPassword); // 입력한 비밀번호가 맞는지


        // 보낼때도 암호화해서 보내면
        // 비밀번호가 일치하지 않는 경우

        if(newPassword != confirmPassword){

        }

        String url="";
        if(myInfoEditDto.getUserProfileImg()!=null) {
            url = imageService.uploadFile(myInfoEditDto.getUserProfileImg());
        }
        myPageService.editProfile(myInfoEditDto, user, url);

        return new ResponseEntity<>(new ResponseMessage(200,"회원정보 수정 성공"),HttpStatus.OK);
    }

//    @PatchMapping("/{userId}") // 마이페이지 정보 수정
//    public ResponseEntity<ResponseMessage> updateProfile(
////            @RequestParam(value = "file") MultipartFile multipartFile,
////            @RequestBody MyInfoEditDto myInfoEditDto,
//            @ModelAttribute MyInfoEditDto myInfoEditDto,
//            @PathVariable Long userId) throws FileUploadException {
//        User user = userService.findUserById(userId);
//        if(user==null)
//            return new ResponseEntity<>(new ResponseMessage(404,"존재하지 않는 유저"),HttpStatus.OK);
//        // 일단 S3에 이미지 업로드 하고 나서
//        String url = imageService.uploadFile(myInfoEditDto.getUserProfileImg());
//        myPageService.editProfile(myInfoEditDto, user, url);
//
//        // 이 url을
//        System.out.println("url = " + url);
//        return new ResponseEntity<>(new ResponseMessage(200,"회원정보 수정 성공"),HttpStatus.OK);
//    }

    @DeleteMapping("/{userId}") // 회원탈퇴
    public ResponseEntity<ResponseMessage> resignMembership(@PathVariable Long userId){
        User user = userService.findUserById(userId);
        if(user==null){
            return new ResponseEntity<>(new ResponseMessage(404,"존재하지 않는 회원에 대한 탈퇴 요청"),HttpStatus.OK);
        }else{
            userService.deleteUserById(userId);
            return new ResponseEntity<>(new ResponseMessage(200,"회원탈퇴 성공"),HttpStatus.OK);
        }
    }

    // 좋아요한 강의 조회
    @GetMapping("/{userId}/liked-lectures")
    public ResponseEntity<ResponseMessage> getLikedLectures(@PathVariable("userId") String userId) {
        Long id = Long.parseLong(userId);
        User user = userService.findUserById(id);
        if(user == null)
            return new ResponseEntity<>(new ResponseMessage(404, "존재하지 않는 유저"), HttpStatus.NOT_FOUND);
        List<LikedLecturesResponse> likedLectures = myPageService.getLikedLectures(user);
        return new ResponseEntity<>(new ResponseMessage(200, "좋아요한 강의 리뷰 조회", likedLectures), HttpStatus.OK);
    }

    // 좋아요한 강의 조회
    @GetMapping("/{userId}/liked-studies")
    public ResponseEntity<ResponseMessage> getLikedStudies(@PathVariable("userId") String userId) {
        Long id = Long.parseLong(userId);
        User user = userService.findUserById(id);
        if(user == null)
            return new ResponseEntity<>(new ResponseMessage(404, "존재하지 않는 유저"), HttpStatus.NOT_FOUND);
        List<LikedStudiesResponse> likedStudies = myPageService.getLikedStudies(user);
        return new ResponseEntity<>(new ResponseMessage(200, "좋아요한 스터디 조회", likedStudies), HttpStatus.OK);
    }

    // 좋아요한 로드맵 조회
    @GetMapping("/{userId}/liked-roadmaps")
    public ResponseEntity<ResponseMessage> getLikedRoadmaps(@PathVariable("userId") String userId) {
        Long id = Long.parseLong(userId);
        User user = userService.findUserById(id);
        if(user == null)
            return new ResponseEntity<>(new ResponseMessage(404, "존재하지 않는 유저"), HttpStatus.NOT_FOUND);
        List<LikedRoadmapsResponse> likedRoadmaps = myPageService.getLikedRoadmaps(user);
        return new ResponseEntity<>(new ResponseMessage(200, "좋아요한 로드맵 조회", likedRoadmaps), HttpStatus.OK);
    }

    // 작성한 강의리뷰 조회
    @GetMapping("/{userId}/reviews")
    public ResponseEntity<ResponseMessage> getMyReviews(@PathVariable("userId") String userId) {
        Long id = Long.parseLong(userId);
        User user = userService.findUserById(id);
        if(user == null)
            return new ResponseEntity<>(new ResponseMessage(404, "존재하지 않는 유저"), HttpStatus.NOT_FOUND);
        List<MyReviewsResponse> myReviews = myPageService.getMyReviews(user);
        return new ResponseEntity<>(new ResponseMessage(200, "작성한 강의리뷰 조회", myReviews), HttpStatus.OK);
    }

    // 작성한 스터디 조회
    @GetMapping("/{userId}/studies")
    public ResponseEntity<ResponseMessage> getMyStudies(@PathVariable("userId") String userId) {
        Long id = Long.parseLong(userId);
        User user = userService.findUserById(id);
        if(user == null)
            return new ResponseEntity<>(new ResponseMessage(404, "존재하지 않는 유저"), HttpStatus.NOT_FOUND);
        List<MyStudiesResponse> myStudies = myPageService.getMyStudies(user);
        return new ResponseEntity<>(new ResponseMessage(200, "작성한 스터디 조회", myStudies), HttpStatus.OK);
    }

    // 스터디 상태변경 _ 마이페이지
    @PatchMapping("/{userId}/studies/{studyId}")
    public ResponseEntity<ResponseMessage> changeRecruitStatus(@PathVariable("userId") String userId, @PathVariable("studyId") String studyId) {
        Long id = Long.parseLong(userId);
        Long studyPostId = Long.parseLong(studyId);
        User user = userService.findUserById(id);
        if (user == null)
            return new ResponseEntity<>(new ResponseMessage(404, "존재하지 않는 유저"), HttpStatus.NOT_FOUND);
        StudyPost study = studyPostService.findStudyPostById(studyPostId);
        if (study == null)
            return new ResponseEntity<>(new ResponseMessage(404, "존재하지 않는 스터디"), HttpStatus.NOT_FOUND);
        if (study.getStudyRecruitStatus() == 1) { // 모집중이라면
            study.updateRecruitStatus(0);
            return new ResponseEntity<>(new ResponseMessage(200, "스터디 모집상태 모집완료로 변경"), HttpStatus.OK);
        } else {
            study.updateRecruitStatus(1);
            return new ResponseEntity<>(new ResponseMessage(200, "스터디 모집상태 모집중으로 변경"), HttpStatus.OK);
        }
    }

    // 작성한 로드맵 조회
    @GetMapping("/{userId}/roadmaps")
    public ResponseEntity<ResponseMessage> getMyRoadmaps(@PathVariable("userId") String userId) {
        Long id = Long.parseLong(userId);
        User user = userService.findUserById(id);
        if(user == null)
            return new ResponseEntity<>(new ResponseMessage(404, "존재하지 않는 유저"), HttpStatus.NOT_FOUND);
        List<MyRoadmapsResponse> myRoadmaps = myPageService.getMyRoadmaps(user);
        return new ResponseEntity<>(new ResponseMessage(200, "작성한 로드맵 조회", myRoadmaps), HttpStatus.OK);
    }
}