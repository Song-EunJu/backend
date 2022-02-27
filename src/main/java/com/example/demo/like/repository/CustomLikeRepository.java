package com.example.demo.like.repository;

import com.example.demo.lecture.Lecture;
import com.example.demo.like.Like;

import java.util.List;

public interface CustomLikeRepository {
    int updateLikeStatus(Like like, int likeStatus);
    List<Like> findLikeByLecture(Lecture lecture); // 강의글 좋아요 가져오기
}