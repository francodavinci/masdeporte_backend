package com.agendalo.services.provider;

import com.agendalo.domain.Provider;
import com.agendalo.domain.User;
import com.agendalo.dto.provider.ProviderDto;
import com.agendalo.repository.ProviderRepository;
import com.agendalo.services.user.UserService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class ProviderService {

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private UserService userService;

    public boolean  createProviderByUserEmail(ProviderDto providerDto) {
        User user = userService.getMyInfo(providerDto.getUserEmail()).getUser();
        if (user == null) {
            return false;
        }

        Provider newProvider = new Provider();
        newProvider.setUser(user);
        providerRepository.save(newProvider);
        return true;
    }

}
