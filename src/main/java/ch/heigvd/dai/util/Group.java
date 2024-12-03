package ch.heigvd.dai.util;

import java.util.LinkedList;

public class Group {
    private String name;
    private int id_owner;
    private int[] id_members;
    private LinkedList<Integer> membersIdList;
    private final int groupId;

    public Group(String name, int id_owner, int[] id_members) {
        this.name = name;
        this.id_owner = id_owner;
        this.id_members = id_members;
        this.groupId = 0;
    }

    public Group(int groupId, int adminId){
        this.groupId = groupId;
        this.id_owner = adminId;
        this.membersIdList = new LinkedList<>();
        membersIdList.add(adminId);
    }

    public int getGroupId() {
        return groupId;
    }

    public LinkedList<Integer> getMembersIdList() {
        return membersIdList;
    }

    public void addMember(int memberId) {
        membersIdList.add(memberId);
    }

    public String name() { return name; }


    public void deleteGroup() {

    }

    public void joinGroup(Integer newMemberId) {
        membersIdList.add(newMemberId);
    }

    @Override
    public String toString() {
        return '[' + this.id_owner + ']' + this.name;
    }
}
