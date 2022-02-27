package com.example.demo.repository;
import com.example.demo.domain.Lecture;
import com.example.demo.domain.Like;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.demo.domain.QLike.like;

@Repository
@RequiredArgsConstructor
public class CustomLikeRepositoryImpl implements CustomLikeRepository{
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public int updateLikeStatus(Like likes, int likeStatus){
        jpaQueryFactory
                .update(like)
                .set(like.likeStatus, likeStatus)
                .where(like.likeId.eq(likes.getLikeId()))
                .execute();
        return likeStatus;
    }

    @Override
    public List<Like> findLikeByLecture(Lecture lecture) {
        return jpaQueryFactory
                .selectFrom(like)
                .where(like.likeStatus.eq(1))
                .fetch();
    }
}
