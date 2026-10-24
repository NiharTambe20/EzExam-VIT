package com.exam.model;

import com.google.gson.annotations.SerializedName;

public class Option {
    private Integer id;

    @SerializedName("question_id")
    private Integer questionId;

    private String text;

    @SerializedName("is_correct")
    private Boolean isCorrect;

    public Option() {}

    public Option(Integer id, Integer questionId, String text, Boolean isCorrect) {
        this.id = id;
        this.questionId = questionId;
        this.text = text;
        this.isCorrect = isCorrect;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
}
