package ch.heigvd.dai.util;

import java.util.ArrayList;
import java.util.LinkedList;

public class Group {
    private final String name;
    private final int id_owner;
    private int[] id_members;
    private LinkedList<Integer> membersIdList;  // liste qui représente les membres du groupes
    private ArrayList<Boolean> listReceived;    // liste qui représente qui a déjà envoyé sa liste

    private ArrayList<User> userList;   // list that combines the two above

    private boolean toBeDeleted = false;
    private boolean onGoingMakeFinal = false;  // indique si l'admin a demandé de faire une liste finale
    private final String password;

    public Group(int adminId, String name, String password){
        this.id_owner = adminId;
        this.name = name;
        this.password = password;
        this.membersIdList = new LinkedList<>();
        membersIdList.add(adminId);
        listReceived = new ArrayList<>();
        listReceived.add(false);

        this.userList = new ArrayList<>();
        userList.add(new User(adminId, false));

    }


    public LinkedList<Integer> getMembersIdList() {
        return membersIdList;
    }

    public void addMember(int memberId) {
        membersIdList.add(memberId);
        listReceived.add(false);

        userList.add(new User(memberId, false));
    }

    public boolean getOnGoingMakeFinal() {
        return onGoingMakeFinal;
    }

    public void setOnGoingMakeFinal(boolean onGoingMakeFinal) {
        this.onGoingMakeFinal = onGoingMakeFinal;
    }

    public void setToBeDeleted(boolean toBeDeleted) {
        this.toBeDeleted = toBeDeleted;
    }

    public boolean getToBeDeleted() {
        return toBeDeleted;
    }

    public void removeMember(int memberId) {
        membersIdList.remove(memberId);
        listReceived.remove(memberId);

        for (User user : userList) {
            if (user.getId() == memberId) {
                userList.remove(user);
                return;
            }
        }
        System.out.println("Weird : couldn't find the desired user in the list");
    }

    public String name() {
        return name;
    }

    public String password(){
        return password;
    }

    public boolean isOwner(int candidateId){
        return id_owner == candidateId;
    }

    public boolean hasMember(int memberId){
        return membersIdList.contains(memberId);
    }

    public boolean everyoneButAdminLeft(){
        return membersIdList.size() == 1 && membersIdList.getFirst() == id_owner;
    }

    public void memberSentList(int clientId){
        for (User user : userList) {
            if (user.getId() == clientId) {
                user.setListReceived(true);
            }
        }
    }

    public boolean everyoneSentList(){
        for (User user : userList) {
            if (!user.listReceived()) return false;
        }
        return true;
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

    public boolean listReceived() {
        return listReceived;
    }
}

