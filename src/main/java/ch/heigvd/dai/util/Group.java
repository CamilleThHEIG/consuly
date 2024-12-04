package ch.heigvd.dai.util;

import java.util.ArrayList;
import java.util.LinkedList;

public class Group {
    private String name;
    private int id_owner;
    private int[] id_members;
    private LinkedList<Integer> membersIdList;  // liste qui représente les membres du groupes
    private ArrayList<Boolean> listReceived;    // liste qui représente qui a déjà envoyé sa liste

    private boolean toBeDeleted;
    private boolean makeFinalList = false;  // indique si l'admin a demandé de faire une liste finale
    private String password;


    public Group(String name, int id_owner, int[] id_members) {
        this.name = name;
        this.id_owner = id_owner;
        this.id_members = id_members;
    }

    public Group(int adminId, String name, String password){
        this.id_owner = adminId;
        this.name = name;
        this.password = password;
        this.membersIdList = new LinkedList<>();
        membersIdList.add(adminId);
        listReceived = new ArrayList<>();
        listReceived.add(false);
    }

    public LinkedList<Integer> getMembersIdList() {
        return membersIdList;
    }

    public void addMember(int memberId) {
        membersIdList.add(memberId);
        listReceived.add(false);
    }

    public void removeMember(int memberId) {
        membersIdList.remove(memberId);
        listReceived.remove(memberId);
    }

    public String name() {
        return name;
    }

    public boolean isOwner(int candidateId){
        return id_owner == candidateId;
    }

    public boolean hasMember(int memberId){
        return membersIdList.contains(memberId);
    }

    public boolean everyoneButAdminLeft(){
        return membersIdList.size() == 1 && membersIdList.get(0) == id_owner;
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
