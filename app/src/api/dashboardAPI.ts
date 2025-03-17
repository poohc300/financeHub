import { fetchRequest } from "../util/fetchRequest";
import { DashboardDataDTO } from "../model/DashboardDataDTO";

export default {
    // GET 요청: 대시보드 데이터 가져오기
    fetchDashboardData() {
        return fetchRequest<DashboardDataDTO[]>("/dashboard/data", "GET");
    },
};
