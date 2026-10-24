package com.exam.model;

import com.google.gson.annotations.SerializedName;

public class Attempt {
    private Integer id;

    @SerializedName("user_id")
    private Integer userId;

    @SerializedName("exam_id")
    private Integer examId;

    private String status; // "in-progress" or "submitted"

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("submitted_at")
    private String submittedAt;

    private Double score;
    private Integer total;
    private Integer correct;
    private Integer wrong;
    private Integer unanswered;
    private Double percentage;
    private Boolean passed;

    @SerializedName("exam_title")
    private String examTitle;

    @SerializedName("student_name")
    private String studentName;

    // Transient fields loaded from exam JOIN to eliminate sequential exam lookups during evaluation
    private transient Double negativeMark;
    private transient Integer passingPercentage;

    public Attempt() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getExamId() {
        return examId;
    }

    public void setExamId(Integer examId) {
        this.examId = examId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(String submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getCorrect() {
        return correct;
    }

    public void setCorrect(Integer correct) {
        this.correct = correct;
    }

    public Integer getWrong() {
        return wrong;
    }

    public void setWrong(Integer wrong) {
        this.wrong = wrong;
    }

    public Integer getUnanswered() {
        return unanswered;
    }

    public void setUnanswered(Integer unanswered) {
        this.unanswered = unanswered;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public String getExamTitle() {
        return examTitle;
    }

    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public Double getNegativeMark() {
        return negativeMark;
    }

    public void setNegativeMark(Double negativeMark) {
        this.negativeMark = negativeMark;
    }

    public Integer getPassingPercentage() {
        return passingPercentage;
    }

    public void setPassingPercentage(Integer passingPercentage) {
        this.passingPercentage = passingPercentage;
    }
}
