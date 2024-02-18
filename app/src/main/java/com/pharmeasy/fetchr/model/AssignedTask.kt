package com.pharmeasy.fetchr.model

data class AssignedTask(
        val verifierTask: Task?,
        val verifierIssue: Task?,
        val rackerTask: Task?,
        val rackerIssue: Task?
)
