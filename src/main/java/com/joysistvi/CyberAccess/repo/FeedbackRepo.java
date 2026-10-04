package com.joysistvi.CyberAccess.repo;

import com.joysistvi.CyberAccess.model.Feedback;

import java.util.List;

public interface FeedbackRepo {

    List<Feedback> getAllFeedback();

    boolean addFeedback(Feedback feedback);

    boolean deleteFeedback(int id);
}