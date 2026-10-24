package com.exam.model;

import com.google.gson.annotations.SerializedName;

public class AttemptAnswer {
    private Integer id;

    @SerializedName("attempt_id")
    private Integer attemptId;

    @SerializedName("question_id")
    private Integer questionId;

    @SerializedName("selected_option_id")
    private Integer selectedOptionId;

    @SerializedName("is_correct")
    private Boolean isCorrect;

    @SerializedName("marks_awarded")
    private Double marksAwarded;

    public AttemptAnswer() {}

    public AttemptAnswer(Integer attemptId, Integer questionId, Integer selectedOptionId, Boolean isCorrect, Double marksAwarded) {
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.selectedOptionId = selectedOptionId;
        this.isCorrect = isCorrect;
        this.marksAwarded = marksAwarded;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public Integer getSelectedOptionId() {
        return selectedOptionId;
    }

    public void setSelectedOptionId(Integer selectedOptionId) {
        this.selectedOptionId = selectedOptionId;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public Double getMarksAwarded() {
        return marksAwarded;
    }

    public void setMarksAwarded(Double marksAwarded) {
        this.marksAwarded = marksAwarded;
    }
}
