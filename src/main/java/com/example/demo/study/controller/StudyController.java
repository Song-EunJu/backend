package com.example.demo.study.controller;

import com.example.demo.dto.*;
import com.example.demo.like.Like;
import com.example.demo.lecture.RecommendService;
import com.example.demo.report.Report;
import com.example.demo.user.UserDetailsServiceImpl;
import com.example.demo.like.LikeService;
import com.example.demo.report.ReportService;
import com.example.demo.study.dto.AllStudyPostsResponse;
import com.example.demo.study.dto.DetailStudyPostResponse;
import com.example.demo.study.dto.StudyPostDTO;
import com.example.demo.study.service.StudyCommentService;
import com.example.demo.study.service.StudyPostService;
import com.example.demo.study.domain.StudyComment;
import com.example.demo.study.domain.StudyPost;
import com.example.demo.user.domain.User;
import com.example.demo.user.dto.SimpleUserDto;
import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;

@Api(tags = { "StudyPost"})
@RestController
@AllArgsConstructor
@Transactional
public class StudyController {

    private final StudyPostService studyPostService;
    private final UserDetailsServiceImpl userDetailsService;
    private final ReportService reportService;
    private final LikeService likeService;
    private final StudyCommentService studyCommentService;
    private final RecommendService recommendService;

    @PersistenceContext
    private final EntityManager em;


    @GetMapping("/studies")
    public ResponseEntity<ResponseMessage> getStudiesByKeyword(@RequestParam(required = false) String keyword, @RequestParam(required = false) String location, @RequestParam(required = false) String category,
                                                               @RequestParam String sort, @RequestParam(required = false) Integer recruitStatus){

        //requestParam : recruitStatus = 1 (모집중) 모집 아니면 null, order = asc (오래된 순), desc (최신순), likes (좋아요 순)
        //pageable 없을 경우: sort 사용 -> 좋아요순 (Sort.by(likeCount), 최신순 (Sort.by(studyPostId, asc) where studyStatus == 1, 오래된 순 (Sort.by(studyPostId, dsc) where studyStatus == 1
       if(keyword==null&&location==null&&category==null){
            List<StudyPost> studyPostList=studyPostService.getAllStudyPosts(recruitStatus, sort);
            if(studyPostList.isEmpty()){
                return new ResponseEntity<>(new ResponseMessage(200,"등록된 스터디글이 없습니다."),HttpStatus.OK);
            }
           List<AllStudyPostsResponse> studiesResponseList=studyPostService.getAllStudiesResponse(studyPostList);
            return new ResponseEntity<>(ResponseMessage.withData(200,"전체 스터디글 조회 성공",studiesResponseList), HttpStatus.OK);
        }

        List<StudyPost> filteredPosts=studyPostService.getStudyPostsWithFilter(category,keyword,location,recruitStatus,sort);
        if(filteredPosts.isEmpty()){
            return new ResponseEntity<>(new ResponseMessage(200,"조건에 맞는 스터디글이 없습니다."),HttpStatus.OK);
        }
        List<AllStudyPostsResponse> filteredResponseList=studyPostService.getAllStudiesResponse(filteredPosts);
        return new ResponseEntity<>(ResponseMessage.withData(200,"스터디글 조회 성공",filteredResponseList), HttpStatus.OK);
    }

    @PostMapping("/studies")
    public ResponseEntity<ResponseMessage> uploadStudyPost(@RequestBody StudyPostDTO postDto, Principal principal){
        //StudyPost 객체를 그대로 반환
        StudyPost newPost = new StudyPost(postDto);

        String email=principal.getName();
        User user=userDetailsService.findUserByEmail(email);
        newPost.setUser(user); // 외래키로 연결된 User를 저장함 ->
        em.persist(newPost);
        studyPostService.saveStudyPost(newPost);

        return new ResponseEntity<>(ResponseMessage.withData(201,"스터디글이 등록 되었습니다.",newPost), HttpStatus.CREATED);
    }

    @GetMapping("/studies/{studyId}")
    public ResponseEntity<ResponseMessage> viewStudyPost(@PathVariable Long studyId,Principal principal){
        StudyPost post=studyPostService.findStudyPostById(studyId);

        if(post!=null&&post.getStudyStatus()==1){
            User loginUser=userDetailsService.findUserByEmail(principal.getName());
            User postUser=post.getUser();
            DetailStudyPostResponse studyPostResponse=new DetailStudyPostResponse(); //상세글을 조회했을 때, 작성자 + 글 핵심정보 + 댓글들 + 댓글들 작성자의 핵심정보를 response 해주기 위한 response 객체
            SimpleUserDto responseUser=userDetailsService.getSimpleUserDto(postUser);
            studyPostResponse.setIsThisUserPostWriter(loginUser.getUserId() == postUser.getUserId());
            studyPostResponse.setStudyPostWriter(responseUser);

            //스터디글에 대한 user 설정 완료

            BeanUtils.copyProperties(post,studyPostResponse); //스터디글 핵심정보만 복사
            studyPostResponse.setStudyRecruitState(post.getStudyRecruitStatus()==1?"모집중":"모집완료"); // 모집중인지 아닌지 텍스트로 return
            studyPostResponse.setLikeCount(likeService.getLikeCountOnStudyPost(post)); //스터디글에 대한 좋아요 개수
            Like userLiked=likeService.findLikeByStudyPostandUser(post,loginUser);
            studyPostResponse.setIsLikedByUser(userLiked!=null&&userLiked.getLikeStatus()==1); //맞으면 true, 아니면 false
            //comment 제외 모든 정보 setting 완료

            List<StudyComment> comments=studyCommentService.findAllParentCommentsOnPosts(post);

            studyPostResponse.setStudyComments(studyCommentService.getAllCommentResponses(comments,loginUser.getUserId(), postUser.getUserId())); //스터디글의 댓글들 목록 (댓글 핵심 내용 + 댓글 작성자의 핵심 정보)

            return new ResponseEntity<>(ResponseMessage.withData(200,"스터디글 찾음",studyPostResponse),HttpStatus.OK);
        }
        return new ResponseEntity<>(new ResponseMessage(404,"존재하지 않는 스터디글에 대한 요청"),HttpStatus.NOT_FOUND);
    }

    @PatchMapping("/studies/{studyId}")
    public ResponseEntity<ResponseMessage> modifyPost(@PathVariable Long studyId, @RequestBody StudyPostDTO postDTO){
        //recruitStatus, repostCount, user, postId, createdDate, studyStatus 제외하고 update
        StudyPost post=studyPostService.modifyStudyPost(postDTO,studyId);
        if(post!=null){
            return new ResponseEntity<>(ResponseMessage.withData(200,"스터디글 수정 성공",post),HttpStatus.OK);
        }else{
            return new ResponseEntity<>(new ResponseMessage(404,"잘못된 수정 요청"),HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/studies/{studyId}")
    public ResponseEntity<ResponseMessage> deletePost(@PathVariable Long studyId){
        StudyPost post=studyPostService.findStudyPostById(studyId);
        if(post!=null){
            post.updateStudyStatus(0); // 0이면 삭제된 글
            studyPostService.saveStudyPost(post); //삭제한 정보를 반영 -> 근데 삭제된 글이면 User가 가지고 있는 글 보여줄 때도 status 0인 글 제외하고 보여줄 수 있겠지?
            return new ResponseEntity<>(new ResponseMessage(200,studyId+"번 글 삭제 성공"),HttpStatus.OK);
        }else{
            return new ResponseEntity<>(new ResponseMessage(404,"존재하지 않는 스터디글에 대한 잘못된 삭제 요청"),HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping ("/studies/{studyId}/reports")
    public ResponseEntity<ResponseMessage> reportPost(@PathVariable Long studyId, @RequestBody HashMap<String, String> params,Principal principal ){
        String content=params.get("reportContent");
        User user=userDetailsService.findUserByEmail(principal.getName());

        StudyPost post=studyPostService.findStudyPostById(studyId);
        Report foundReport=reportService.findByUserAndStudyPost(user,post);
        if(foundReport!=null){
            return new ResponseEntity<>(new ResponseMessage(409,"이미 신고한 스터디글"),HttpStatus.OK);
        }
        Report report=new Report(content,post,user);
        reportService.saveReport(report);

        Integer reportCount=post.getStudyReportCount();
        post.updateStudyReportCount(++reportCount);

        if(reportCount==5){
            post.updateStudyStatus(0); // 5번 신고된 글은 삭제 처리
            studyPostService.saveStudyPost(post);
            return new ResponseEntity<>(new ResponseMessage(200,studyId+"번 스터디글은 신고가 5번 누적되어 삭제되었습니다."),HttpStatus.OK);
        }

        studyPostService.saveStudyPost(post); // 신고내역 update 후 저장
        return new ResponseEntity<>(new ResponseMessage(200,studyId+"번 스터디글 신고 완료"),HttpStatus.OK);
    }

    @PostMapping("/studies/{studyId}/likes")
    public ResponseEntity<ResponseMessage> likeStudy(@PathVariable Long studyId,Principal principal){
        String userEmail=principal.getName();
        User user=userDetailsService.findUserByEmail(userEmail);

        StudyPost post=studyPostService.findStudyPostById(studyId);
        if(post == null) // 스터디가 없는 경우
            return new ResponseEntity<>(new ResponseMessage(404, "해당하는 스터디가 없습니다"), HttpStatus.NOT_FOUND);

        Like like = likeService.findLikeByStudyPostandUser(post,user);
        if(like ==null){
            //최초 좋아요 등록
            like =new Like(user,post);
            likeService.saveLike(like);
            return new ResponseEntity<>(new ResponseMessage(201,studyId+"번 스터디글 좋아요 등록 성공"),HttpStatus.CREATED); // 아놕 왜 좋아요 누른 post 정보가 같이 안보내질까,,, 안보내줘도 되나??
        }else if(like.getLikeStatus()==0){
            //좋아요 누른 데이터가 있는데 좋아요가 취소된 상태라면 다시 좋아요 설정
            like.setLikeStatus(1);
            likeService.saveLike(like);
            return new ResponseEntity<>(new ResponseMessage(200,studyId+"번 스터디글 좋아요로 상태 변경 성공"),HttpStatus.OK);
        }else{
            //좋아요 누른 데이터가 있는데 좋아요가 눌려있는 상태 -> 좋아요를 취소해줘야함
            like.setLikeStatus(0);
            likeService.saveLike(like);
            return new ResponseEntity<>(new ResponseMessage(200,studyId+"번 스터디글 좋아요 취소 성공"),HttpStatus.OK);
        }

    }





}
