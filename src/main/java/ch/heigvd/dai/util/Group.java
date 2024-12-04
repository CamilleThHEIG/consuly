package ch.heigvd.dai.util;

import java.util.ArrayList;
import java.util.LinkedList;

public class Group {
    private String name;
    private int id_owner;
    private int[] id_members;
    private LinkedList<Integer> membersIdList;  // liste qui représente les membres du groupes
    private ArrayList<Boolean> listReceived;    // liste qui représente qui a déjà envoyé sa liste
    private final int groupId;
    private boolean makeFinalList = false;  // indique si l'admin a demandé de faire une liste finale

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
        listReceived = new ArrayList<>();
        listReceived.add(false);
    }

    public int getGroupId() {
        return groupId;
    }

    public LinkedList<Integer> getMembersIdList() {
        return membersIdList;
    }

    public void addMember(int memberId) {
        membersIdList.add(memberId);
        listReceived.add(false);
    }

    public String name() {
        return name;
    }

    public boolean getMakeFinalList() {
        return makeFinalList;
    }

    public void setMakeFinalList(boolean makeFinalList) {
        this.makeFinalList = makeFinalList;
    }

    public void notifiyListReceived(int memberId) {
        // vérifier que le memberId est bien membre du groupe
        int index = membersIdList.indexOf(memberId);

        listReceived.set(index, true);

        System.out.println(listReceived.size());
        for (Boolean newB : listReceived) {
            System.out.println(newB);
        }
    }

    public boolean allListReceived(){
        for (Boolean b : listReceived){
            if (!b) return false;
        }
        return true;
    }

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

/*  Test class
class User {
    private final int id;
    private boolean listReceived;
    User(int id, boolean listReceived) {
        this.id = id;
        this.listReceived = listReceived;
    }

    public int getId() {
        return id;
    }

    public void setListReceived(boolean listReceived) {
        this.listReceived = listReceived;
    }

    public boolean isListReceived() {
        return listReceived;
    }
}
*/
